package callback

import (
	"context"
	"sync"

	"net/http"

	"time"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/pos"

	"github.com/golang-jwt/jwt/v5"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/thirdparty/moneropay"
)

type CallbackService struct {
	repo      CallbackRepository
	config    *config.Config
	moneroPay *moneropay.MoneroPayAPIClient
	mu        sync.Mutex
}

func NewCallbackService(repo CallbackRepository, cfg *config.Config, moneroPay *moneropay.MoneroPayAPIClient) *CallbackService {
	return &CallbackService{repo: repo, config: cfg, moneroPay: moneroPay}
}

func (s *CallbackService) StartConfirmationChecker(ctx context.Context, interval time.Duration) {
	go func() {
		ticker := time.NewTicker(interval)
		defer ticker.Stop()
		for {
			select {
			case <-ticker.C:
				s.checkUnconfirmedTransactions()
			case <-ctx.Done():
				return
			}
		}
	}()
}

// This method queries for unconfirmed transactions and checks MoneroPay
func (s *CallbackService) checkUnconfirmedTransactions() {
	s.mu.Lock()
	defer s.mu.Unlock()

	unconfirmed, err := s.repo.FindUnconfirmedTransactions()
	if err != nil {
		return
	}

	for _, tx := range unconfirmed {
		moneroStatus, err := s.moneroPay.GetReceiveAddress(*tx.SubAddress, &moneropay.GetReceiveAddressParams{})
		if err != nil {
			continue
		}

		if moneroStatus != nil {
			_ = s.processTransaction(tx.ID, *moneroStatus)
		}
	}
}

func (s *CallbackService) processTransaction(transactionID uint, transactionToProcess moneropay.ReceiveAddressResponse) *models.HTTPError {

	// Get the transaction by ID
	transaction, err := s.repo.FindTransactionByID(transactionID)
	if err != nil {
		return models.NewHTTPError(http.StatusNotFound, "Transaction not found")
	}

	for _, subTxToProcess := range transactionToProcess.Transactions {
		// Create or update the subtransaction
		subTransaction := &models.SubTransaction{
			TransactionID:   transaction.ID,
			Amount:          subTxToProcess.Amount,
			Confirmations:   subTxToProcess.Confirmations,
			DoubleSpendSeen: subTxToProcess.DoubleSpendSeen,
			Fee:             subTxToProcess.Fee,
			Height:          subTxToProcess.Height,
			Timestamp:       subTxToProcess.Timestamp,
			TxHash:          subTxToProcess.TxHash,
			UnlockTime:      subTxToProcess.UnlockTime,
			Locked:          subTxToProcess.Locked,
		}

		// See if the txHash already exists in the transaction's subtransactions
		existing := false
		for _, subTx := range transaction.SubTransactions {
			if subTx.TxHash == subTransaction.TxHash {
				subTransaction.ID = subTx.ID // Ensure we set the ID for update
				existing = true
				break
			}
		}

		if !existing {
			// Create new subtransaction
			_, err := s.repo.CreateSubTransaction(subTransaction)
			if err != nil {
				return models.NewHTTPError(http.StatusInternalServerError, "Failed to create subtransaction: "+err.Error())
			}
		} else {
			// Update existing subtransaction
			_, err := s.repo.UpdateSubTransaction(subTransaction)
			if err != nil {
				return models.NewHTTPError(http.StatusInternalServerError, "Failed to update subtransaction: "+err.Error())
			}
		}
	}

	// Get the updated transaction with subtransactions
	transaction, err = s.repo.FindTransactionByID(transaction.ID)
	if err != nil {
		return models.NewHTTPError(http.StatusNotFound, "Transaction not found after update")
	}

	// Calculate if transaction is accepted
	allAccepted := true
	for _, subTx := range transaction.SubTransactions {
		if subTx.Confirmations < transaction.RequiredConfirmations {
			allAccepted = false
			break
		}
	}

	if transactionToProcess.Amount.Covered.Total < transaction.Amount {
		allAccepted = false
	}

	transaction.Accepted = allAccepted

	// Calculate if the transaction is confirmed
	allConfirmed := true
	for _, subTx := range transaction.SubTransactions {
		if subTx.Confirmations < 10 {
			allConfirmed = false
			break
		}
	}

	if transactionToProcess.Amount.Covered.Unlocked < transaction.Amount {
		allConfirmed = false
	}

	transaction.Confirmed = allConfirmed

	// Update the transaction in the repository
	_, err = s.repo.UpdateTransaction(transaction)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "Failed to update transaction: "+err.Error())
	}

	pos.NotifyTransactionUpdate(transaction.ID, transaction)

	return nil
}

func (s *CallbackService) HandleCallback(jwtToken string, callback moneropay.CallbackResponse) (httpErr *models.HTTPError) {
	s.mu.Lock()
	defer s.mu.Unlock()

	// Validate JWT
	if jwtToken == "" {
		return models.NewHTTPError(http.StatusUnauthorized, "JWT is required")
	}

	type Claims struct {
		TransactionID uint `json:"transaction_id"`
		jwt.RegisteredClaims
	}
	claims := &Claims{}
	token, err := jwt.ParseWithClaims(jwtToken, claims, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, models.NewHTTPError(http.StatusUnauthorized, "invalid signing method")
		}
		return []byte(s.config.JWTMoneroPaySecret), nil
	})

	if err != nil {
		return models.NewHTTPError(http.StatusUnauthorized, "Invalid token: "+err.Error())
	}

	if !token.Valid {
		return models.NewHTTPError(http.StatusUnauthorized, "Invalid token")
	}

	httpErr = s.processTransaction(claims.TransactionID, callback.ToReceiveAddressResponse())
	if httpErr != nil {
		return httpErr
	}
	return nil
}
