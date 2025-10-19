package vendor

import (
	"context"
	"log"
	"net/http"
	"sync"
	"time"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/rpc"
	"golang.org/x/crypto/bcrypt"
	"gorm.io/gorm"
)

type VendorService struct {
	repo      VendorRepository
	db        *gorm.DB
	config    *config.Config
	rpcClient *rpc.Client
	mu        sync.Mutex
}

func NewVendorService(repo VendorRepository, db *gorm.DB, cfg *config.Config, rpcClient *rpc.Client) *VendorService {
	return &VendorService{repo: repo, db: db, config: cfg, rpcClient: rpcClient}
}

func (s *VendorService) StartTransferCompleter(ctx context.Context, interval time.Duration) {
	go func() {
		ticker := time.NewTicker(interval)
		defer ticker.Stop()
		for {
			select {
			case <-ticker.C:
				// bound each sweep to avoid piling up
				sweepCtx, cancel := context.WithTimeout(ctx, 30*time.Second)
				s.completeTransfers(sweepCtx)
				cancel()
			case <-ctx.Done():
				return
			}
		}
	}()
}

func (s *VendorService) completeTransfers(ctx context.Context) {
	s.mu.Lock()
	defer s.mu.Unlock()

	// We use transfer intead of transfer_split because we need support for subtract_fee_from_outputs
	// We need that because we do not want the server operator to be responsible for covering transaction fees
	// When transfer_split is supported, we can switch to it for a much more efficient transfer process

	// For loop to try and complete transfers
	for i := 15; i > 0; i-- {
		dbTx := s.db.Begin()
		func() {
			defer func() {
				if r := recover(); r != nil {
					_ = dbTx.Rollback()
				}
			}()
			// fetch a safe number of transfers to complete
			transfers, err := s.repo.GetTransfersToComplete(ctx, i)
			if err != nil || transfers == nil {
				log.Println("Error fetching transfers to complete:", err)
				_ = dbTx.Rollback()
				return
			}

			if len(transfers) == 0 {
				_ = dbTx.Rollback()
				return
			}

			// Mark transactions as transferred
			for _, transfer := range transfers {
				transactionIDs := []uint{}
				for _, tx := range transfer.Transactions {
					transactionIDs = append(transactionIDs, tx.ID)
				}
				if err := s.repo.MarkTransactionsTransferred(ctx, dbTx, transfer.ID, transactionIDs); err != nil {
					log.Printf("Error marking transactions as transferred: %v", err)
					_ = dbTx.Rollback()
					return
				}
			}

			// Format RPC call

			type Destination struct {
				Amount  int64  `json:"amount"`
				Address string `json:"address"`
			}

			type TransferParams struct {
				Destinations           []Destination `json:"destinations"`
				SubtractFeeFromOutputs []uint        `json:"subtract_fee_from_outputs,omitempty"`
				DoNotRelay             bool          `json:"do_not_relay,omitempty"`
				Priority               uint          `json:"priority,omitempty"`
			}

			destinations := make([]Destination, len(transfers))
			subtractFeeFromOutputs := make([]uint, 0, len(transfers))
			for i, transfer := range transfers {
				destinations[i] = Destination{
					Amount:  transfer.Amount,
					Address: transfer.Address,
				}
				subtractFeeFromOutputs = append(subtractFeeFromOutputs, uint(i))
			}

			params := TransferParams{
				Destinations:           destinations,
				SubtractFeeFromOutputs: subtractFeeFromOutputs,
				DoNotRelay:             true,
				Priority:               0,
			}

			type TransferResult struct {
				Amount        int64 `json:"amount"`
				AmountsByDest struct {
					Amounts []int64 `json:"amounts"`
				} `json:"amounts_by_dest"`
				Fee            int64  `json:"fee"`
				MultisigTxset  string `json:"multisig_txset"`
				SpentKeyImages struct {
					KeyImages []string `json:"key_images"`
				} `json:"spent_key_images"`
				TxBlob        string `json:"tx_blob"`
				TxHash        string `json:"tx_hash"`
				TxKey         string `json:"tx_key"`
				TxMetadata    string `json:"tx_metadata"`
				UnsignedTxset string `json:"unsigned_txset"`
				Weight        int64  `json:"weight"`
			}

			// Transfer funds without relaying to test that all fits inside single transfer
			var testResult TransferResult
			// per-call timeout bound to sweep
			callCtx, cancel := context.WithTimeout(ctx, 10*time.Second)
			err = s.rpcClient.Call(callCtx, "transfer", params, &testResult)
			cancel()
			if err != nil {
				log.Printf("RPC error: %v", err)
				_ = dbTx.Rollback()
				return
			} else {
				log.Printf("Test transfer result: %+v", testResult)
			}

			// If we reach here, it means the test transfer was successful

			// Now transfer the funds for real

			params.DoNotRelay = false // We want to relay the transaction now

			var result TransferResult
			callCtx2, cancel2 := context.WithTimeout(ctx, 15*time.Second)
			err = s.rpcClient.Call(callCtx2, "transfer", params, &result)
			cancel2()
			if err != nil {
				log.Printf("RPC error during transfer: %v", err)
				_ = dbTx.Rollback()
				return
			}
			log.Printf("Transfer result: %+v", result)
			if result.TxHash == "" {
				log.Print("Transfer failed, no transaction hash returned")
				_ = dbTx.Rollback()
				return
			}
			// We need to mark the transfer as completed

			for index, amount := range result.AmountsByDest.Amounts {
				if err := s.repo.MarkTransferCompleted(ctx, dbTx, transfers[index].ID, amount, result.TxHash); err != nil {
					log.Println("Error marking transfer as completed:", err)
				}
			}
			if err := dbTx.Commit().Error; err != nil {
				log.Println("Error committing transaction:", err)
				return
			}
			log.Println("Transfer completed successfully")
			return
		}() // end per-iteration scope
	}

}

func (s *VendorService) CreateVendor(ctx context.Context, name string, password string, inviteCode string) (httpErr *models.HTTPError) {

	if len(name) < 3 || len(name) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "name must be at least 3 characters and no more than 50 characters")
	}

	if len(password) < 8 || len(password) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "password must be at least 8 characters and no more than 50 characters")
	}

	nameTaken, err := s.repo.VendorByNameExists(ctx, name)

	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error checking if vendor name exists: "+err.Error())
	}

	if nameTaken {
		return models.NewHTTPError(http.StatusBadRequest, "vendor name already taken")
	}

	invite, err := s.repo.FindInviteByCode(ctx, inviteCode)
	if err != nil {
		return models.NewHTTPError(http.StatusBadRequest, "invalid invite code")
	}

	if invite.Used {
		return models.NewHTTPError(http.StatusBadRequest, "invite code already used")
	}

	if invite.ForcedName != nil && *invite.ForcedName != name {
		return models.NewHTTPError(http.StatusBadRequest, "invite code is for a different name")
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error hashing password: "+err.Error())
	}

	vendor := &models.Vendor{
		Name:         name,
		PasswordHash: string(hashedPassword),
	}

	err = s.repo.CreateVendor(ctx, vendor)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error creating vendor: "+err.Error())
	}

	err = s.repo.SetInviteToUsed(ctx, invite.ID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error setting invite to used: "+err.Error())
	}

	return nil
}

func (s *VendorService) DeleteVendor(ctx context.Context, vendorID uint) (httpErr *models.HTTPError) {
	vendor, err := s.repo.GetVendorByID(ctx, vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error retrieving vendor: "+err.Error())
	}

	if vendor == nil {
		return models.NewHTTPError(http.StatusNotFound, "vendor not found")
	}

	if vendor.Balance != 0 {
		return models.NewHTTPError(http.StatusBadRequest, "vendor balance must be 0 to delete vendor")
	}

	err = s.repo.DeleteAllPosForVendor(ctx, vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error deleting POS for vendor: "+err.Error())
	}

	err = s.repo.DeleteAllTransactionsForVendor(ctx, vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error deleting transactions for vendor: "+err.Error())
	}

	err = s.repo.DeleteVendor(ctx, vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error deleting vendor: "+err.Error())
	}

	return nil
}

func (s *VendorService) CreatePos(ctx context.Context, name string, password string, vendorID uint) (httpErr *models.HTTPError) {

	if len(name) < 3 || len(name) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "name must be at least 3 characters and no more than 50 characters")
	}

	if len(password) < 8 || len(password) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "password must be at least 8 characters and no more than 50 characters")
	}

	nameTaken, err := s.repo.PosByNameExistsForVendor(ctx, name, vendorID)

	if err != nil {
		return models.NewHTTPError(http.StatusBadRequest, "error checking if POS name exists: "+err.Error())
	}

	if nameTaken {
		return models.NewHTTPError(http.StatusBadRequest, "POS name already taken")
	}

	// check to see if vendor still exists. This is to prevent POS creation on deleted vendor, but probably needs to be done in a better way
	vendor, err := s.repo.GetVendorByID(ctx, vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusBadRequest, "error retrieving vendor: "+err.Error())
	}

	if vendor == nil {
		return models.NewHTTPError(http.StatusBadRequest, "vendor not found")
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error hashing password: "+err.Error())
	}

	pos := &models.Pos{
		Name:         name,
		PasswordHash: string(hashedPassword),
		VendorID:     vendorID,
	}

	err = s.repo.CreatePos(ctx, pos)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error creating POS: "+err.Error())
	}

	return nil
}

func (s *VendorService) GetBalance(ctx context.Context, vendorID uint) (*int64, *models.HTTPError) {
	balance, err := s.repo.GetBalance(ctx, vendorID)
	if err != nil {
		return nil, models.NewHTTPError(http.StatusInternalServerError, "error retrieving balance: "+err.Error())
	}

	return &balance, nil
}

func (s *VendorService) CreateTransfer(ctx context.Context, vendorID uint, address string) *models.HTTPError {
	s.mu.Lock()
	defer s.mu.Unlock()

	// Check if vendor already has a transfer in progress
	transfer, err := s.repo.GetActiveTransferByVendorID(ctx, vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "DB error: "+err.Error())
	}
	if transfer != nil {
		return models.NewHTTPError(http.StatusBadRequest, "Transfer already in progress for this vendor")
	}

	/* var transactionIDs []uint */

	transactions, err := s.repo.GetAllTransferableTransactions(ctx, vendorID)

	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "DB error: "+err.Error())
	}

	if len(transactions) == 0 {
		return models.NewHTTPError(http.StatusBadRequest, "No transferable transactions found for this vendor")
	}

	totalAmount := int64(0)

	for _, tx := range transactions {
		totalAmount += tx.Amount
	}

	// Do not allow withdrawals of less than 0.003 XMR as the fee is too high
	if totalAmount < 3000000 {
		return models.NewHTTPError(http.StatusBadRequest, "Minimum transfer amount is 0.003 XMR")
	}

	// Create a new transfer record
	newTransfer := &models.Transfer{
		VendorID:     vendorID,
		Amount:       totalAmount,
		Address:      address,
		Transactions: transactions,
	}

	err = s.repo.CreateTransfer(ctx, newTransfer)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "DB error: "+err.Error())
	}

	return nil
}
