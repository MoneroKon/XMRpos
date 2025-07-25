package vendor

import (
	/* "log" */
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
				s.completeTransfers()
			case <-ctx.Done():
				return
			}
		}
	}()
}

func (s *VendorService) completeTransfers() {
	s.mu.Lock()
	defer s.mu.Unlock()

	// We use transfer intead of transfer_split because we need support for subtract_fee_from_outputs
	// We need that because we do not want the server operator to be responsible for covering transaction fees
	// When transfer_split is supported, we can switch to it for a much more efficient transfer process

	// For loop to try and complete transfers
	for i := 15; i > 0; i-- {
		dbTx := s.db.Begin()
		defer func() {
			if r := recover(); r != nil {
				dbTx.Rollback()
			}
		}()
		// fetch a safe number of transfers to complete
		transfers, err := s.repo.GetTransfersToComplete(i)
		if err != nil {
			log.Println("Error fetching transfers to complete:", err)
			return
		}

		if len(transfers) == 0 {
			return
		}

		// Mark transactions as transferred
		for _, transfer := range transfers {
			transactionIDs := []uint{}
			for _, tx := range transfer.Transactions {
				transactionIDs = append(transactionIDs, tx.ID)
			}
			if err := s.repo.MarkTransactionsTransferred(dbTx, transfer.ID, transactionIDs); err != nil {
				log.Println("Error marking transactions as transferred:", err)
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

		// Transfer funds without relaying to test that all fits inside single transfer.
		var testResult TransferResult
		err = s.rpcClient.Call("transfer", params, &testResult)
		if err != nil {
			log.Println("RPC error:", err)
			dbTx.Rollback()
			continue
		} else {
			log.Printf("Test transfer result: %+v", testResult)
		}

		// If we reach here, it means the test transfer was successful

		// Now transfer the funds for real

		params.DoNotRelay = false // We want to relay the transaction now

		var result TransferResult
		err = s.rpcClient.Call("transfer", params, &result)
		if err != nil {
			log.Println("RPC error during transfer:", err)
			dbTx.Rollback()
			continue
		}
		log.Printf("Transfer result: %+v", result)
		if result.TxHash == "" {
			log.Println("Transfer failed, no transaction hash returned")
			dbTx.Rollback()
			continue
		}
		// We need to mark the transfer as completed

		for index, amount := range result.AmountsByDest.Amounts {
			if err := s.repo.MarkTransferCompleted(dbTx, transfers[index].ID, amount, result.TxHash); err != nil {
				log.Println("Error marking transfer as completed:", err)
			}
		}
		if err := dbTx.Commit().Error; err != nil {
			log.Println("Error committing transaction:", err)
			return
		}
		log.Println("Transfer completed successfully")
		return
	}

}

func (s *VendorService) CreateVendor(name string, password string, inviteCode string) (httpErr *models.HTTPError) {

	if len(name) < 3 || len(name) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "name must be at least 3 characters and no more than 50 characters")
	}

	if len(password) < 8 || len(password) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "password must be at least 8 characters and no more than 50 characters")
	}

	nameTaken, err := s.repo.VendorByNameExists(name)

	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error checking if vendor name exists: "+err.Error())
	}

	if nameTaken {
		return models.NewHTTPError(http.StatusBadRequest, "vendor name already taken")
	}

	invite, err := s.repo.FindInviteByCode(inviteCode)
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

	err = s.repo.CreateVendor(vendor)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error creating vendor: "+err.Error())
	}

	err = s.repo.SetInviteToUsed(invite.ID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error setting invite to used: "+err.Error())
	}

	return nil
}

func (s *VendorService) DeleteVendor(vendorID uint) (httpErr *models.HTTPError) {
	vendor, err := s.repo.GetVendorByID(vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error retrieving vendor: "+err.Error())
	}

	if vendor == nil {
		return models.NewHTTPError(http.StatusNotFound, "vendor not found")
	}

	if vendor.Balance != 0 {
		return models.NewHTTPError(http.StatusBadRequest, "vendor balance must be 0 to delete vendor")
	}

	err = s.repo.DeleteAllPosForVendor(vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error deleting POS for vendor: "+err.Error())
	}

	err = s.repo.DeleteAllTransactionsForVendor(vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error deleting transactions for vendor: "+err.Error())
	}

	err = s.repo.DeleteVendor(vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error deleting vendor: "+err.Error())
	}

	return nil
}

func (s *VendorService) CreatePos(name string, password string, vendorID uint) (httpErr *models.HTTPError) {

	if len(name) < 3 || len(name) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "name must be at least 3 characters and no more than 50 characters")
	}

	if len(password) < 8 || len(password) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "password must be at least 8 characters and no more than 50 characters")
	}

	nameTaken, err := s.repo.PosByNameExistsForVendor(name, vendorID)

	if err != nil {
		return models.NewHTTPError(http.StatusBadRequest, "error checking if POS name exists: "+err.Error())
	}

	if nameTaken {
		return models.NewHTTPError(http.StatusBadRequest, "POS name already taken")
	}

	// check to see if vendor still exists. This is to prevent POS creation on deleted vendor, but probably needs to be done in a better way
	vendor, err := s.repo.GetVendorByID(vendorID)
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

	err = s.repo.CreatePos(pos)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error creating POS: "+err.Error())
	}

	return nil
}

func (s *VendorService) GetBalance(vendorID uint) (*int64, *models.HTTPError) {
	balance, err := s.repo.GetBalance(vendorID)
	if err != nil {
		return nil, models.NewHTTPError(http.StatusInternalServerError, "error retrieving balance: "+err.Error())
	}

	return &balance, nil
}

func (s *VendorService) CreateTransfer(vendorID uint, address string) *models.HTTPError {
	s.mu.Lock()
	defer s.mu.Unlock()

	// Check if vendor already has a transfer in progress
	transfer, err := s.repo.GetActiveTransferByVendorID(vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "DB error: "+err.Error())
	}
	if transfer != nil {
		return models.NewHTTPError(http.StatusBadRequest, "Transfer already in progress for this vendor")
	}

	/* var transactionIDs []uint */

	transactions, err := s.repo.GetAllTransferableTransactions(vendorID)

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

	err = s.repo.CreateTransfer(newTransfer)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "DB error: "+err.Error())
	}

	return nil
}
