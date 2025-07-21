package callback

import (
	"net/http"

	"github.com/golang-jwt/jwt/v5"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/thirdparty/moneropay"
)

type CallbackService struct {
	repo   CallbackRepository
	config *config.Config
}

func NewCallbackService(repo CallbackRepository, cfg *config.Config) *CallbackService {
	return &CallbackService{repo: repo, config: cfg}
}

func (s *CallbackService) HandleCallback(jwtToken string, callback moneropay.ReceiveAddressResponse) (httpErr *models.HTTPError) {

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

	// Update the transaction with the new data from the callback
	transaction, err := s.repo.FindTransactionByID(claims.TransactionID)
	if err != nil {
		return models.NewHTTPError(http.StatusNotFound, "Transaction not found")
	}

	// Transform the callback transactions into subtransactions

	transaction.SubTransactions = []models.SubTransaction{} // Reset subtransactions

	for _, tx := range callback.Transactions {
		subTx := models.SubTransaction{
			TransactionID:   transaction.ID,
			Amount:          tx.Amount,
			Confirmations:   tx.Confirmations,
			DoubleSpendSeen: tx.DoubleSpendSeen,
			Fee:             tx.Fee,
			Height:          tx.Height,
			Timestamp:       tx.Timestamp,
			TxHash:          tx.TxHash,
			UnlockTime:      tx.UnlockTime,
			Locked:          tx.Locked,
		}
		transaction.SubTransactions = append(transaction.SubTransactions, subTx)
	}

	// Calculate if transaction is accepted
	// If all of the subtransactions have the required confirmations and the covered amount is sufficient, mark the transaction as accepted
	allAccepted := true
	for _, subTx := range transaction.SubTransactions {
		if subTx.Confirmations < transaction.RequiredConfirmations {
			allAccepted = false
			break
		}
	}

	if callback.Amount.Covered.Total < transaction.Amount {
		allAccepted = false
	}

	transaction.Accepted = allAccepted

	// Calculate if the transaction is confirmed
	// If all of the subtransactions have 10+ confirmations and the covering amount is sufficient, mark the transaction as confirmed
	allConfirmed := true
	for _, subTx := range transaction.SubTransactions {
		if subTx.Confirmations < 10 {
			allConfirmed = false
			break
		}
	}

	if callback.Amount.Covered.Unlocked < transaction.Amount {
		allConfirmed = false
	}

	transaction.Confirmed = allConfirmed

	// Update the transaction in the repository
	updatedTransaction, err := s.repo.UpdateTransaction(transaction)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "Failed to update transaction: "+err.Error())
	}
	if updatedTransaction == nil {
		return models.NewHTTPError(http.StatusInternalServerError, "Transaction not found")
	}

	return nil
}
