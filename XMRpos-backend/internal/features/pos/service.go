package pos

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/thirdparty/moneropay"
)

type PosService struct {
	repo      PosRepository
	config    *config.Config
	moneroPay *moneropay.MoneroPayAPIClient
}

const moneroAtomicUnitsPerXMR int64 = 1_000_000_000_000

var ErrNoConfirmedTransactions = errors.New("no confirmed transactions in DB")

func NewPosService(repo PosRepository, cfg *config.Config, moneroPay *moneropay.MoneroPayAPIClient) *PosService {
	return &PosService{repo: repo, config: cfg, moneroPay: moneroPay}
}

type ConfirmedTransactionSummary struct {
	TransactionID uint      `json:"transaction_id"`
	TxHash        string    `json:"tx_hash"`
	Timestamp     time.Time `json:"timestamp"`
	Height        int64     `json:"height"`
	Accepted      bool      `json:"accepted"`
	Confirmed     bool      `json:"confirmed"`
}

type PendingTransactionSummary struct {
	ID        uint  `json:"id"`
	Amount    int64 `json:"amount"`
	Accepted  bool  `json:"accepted"`
	Confirmed bool  `json:"confirmed"`
}

type ListTransactionsResult struct {
	Confirmed []ConfirmedTransactionSummary `json:"confirmed_transactions"`
	Pending   []PendingTransactionSummary   `json:"pending_transactions"`
}

func (s *PosService) CreateTransaction(ctx context.Context, vendorID uint, posID uint, amount int64, description *string, amountInCurrency float64, currency string, requiredConfirmations int64) (id uint, address string, err error) {
	if ctx == nil {
		ctx = context.Background()
	}

	transaction := &models.Transaction{
		VendorID:              vendorID,
		PosID:                 posID,
		Amount:                amount,
		RequiredConfirmations: requiredConfirmations,
		Currency:              currency,
		AmountInCurrency:      amountInCurrency,
		Description:           description,
	}

	transactionDB, err := s.repo.CreateTransaction(ctx, transaction)
	if err != nil {
		return 0, "", err
	}

	// Create a jwt token for the transaction which contains the transaction ID
	moneroPayTokenJWT := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"transaction_id": transactionDB.ID,
		"exp":            time.Now().Add(time.Hour * 6).Unix(),
	})

	accessToken, err := moneroPayTokenJWT.SignedString([]byte(s.config.JWTMoneroPaySecret))
	if err != nil {
		return 0, "", err
	}

	callbackURLTemplate := s.config.MoneroPayCallbackURL
	var callbackUrl string
	if strings.Contains(callbackURLTemplate, "{jwt}") {
		callbackUrl = strings.Replace(callbackURLTemplate, "{jwt}", accessToken, 1)
	} else {
		callbackUrl = strings.TrimRight(callbackURLTemplate, "/") + "/receive/" + accessToken
	}

	var desc string
	if description != nil {
		desc = *description
	}

	req := &moneropay.ReceiveRequest{
		Amount:      amount,
		Description: desc,
		CallbackUrl: callbackUrl,
	}

	// per-call timeout for external dependency
	callCtx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	resp, err := s.moneroPay.PostReceive(callCtx, req)
	if err != nil {
		return 0, "", err
	}

	// Update the transaction with the subaddress received from MoneroPay
	transactionDB.SubAddress = &resp.Address
	if _, err := s.repo.UpdateTransaction(ctx, transactionDB); err != nil {
		return 0, "", err
	}

	return transactionDB.ID, resp.Address, nil
}

// GetTransaction retrieves a transaction by its ID if authorized
func (s *PosService) GetTransaction(ctx context.Context, transactionID uint, vendorID uint, posID uint) (transaction *models.Transaction, httpErr *models.HTTPError) {
	// Find the transaction by ID
	if ctx == nil {
		ctx = context.Background()
  }
	transaction, err := s.repo.FindTransactionByID(ctx, transactionID)
	if err != nil {
		return nil, models.NewHTTPError(404, "Transaction not found")
	}

	// Check if the vendor and POS are authorized for the transaction
	if !s.IsAuthorizedForTransaction(vendorID, posID, transaction) {
		return nil, models.NewHTTPError(403, "Unauthorized for this transaction")
	}

	return transaction, nil
}

// Check if the vendor and POS are authorized for the transaction
func (s *PosService) IsAuthorizedForTransaction(vendorID uint, posID uint, transaction *models.Transaction) bool {
	if transaction.VendorID != vendorID || transaction.PosID != posID {
		return false
	}
	return true
}

func (s *PosService) ListTransactionsByPos(ctx context.Context, vendorID uint, posID uint) (*ListTransactionsResult, error) {
	if ctx == nil {
		ctx = context.Background()
	}

	transactions, err := s.repo.FindTransactionsByPosID(ctx, vendorID, posID)
	if err != nil {
		return nil, err
	}

	result := &ListTransactionsResult{
		Confirmed: make([]ConfirmedTransactionSummary, 0),
		Pending:   make([]PendingTransactionSummary, 0),
	}

	for _, transaction := range transactions {
		if transaction.Confirmed {
			for _, sub := range transaction.SubTransactions {
				result.Confirmed = append(result.Confirmed, ConfirmedTransactionSummary{
					TransactionID: sub.TransactionID,
					TxHash:        sub.TxHash,
					Timestamp:     sub.Timestamp,
					Height:        sub.Height,
					Accepted:      transaction.Accepted,
					Confirmed:     transaction.Confirmed,
				})
			}
			continue
		}

		result.Pending = append(result.Pending, PendingTransactionSummary{
			ID:        transaction.ID,
			Amount:    transaction.Amount,
			Accepted:  transaction.Accepted,
			Confirmed: transaction.Confirmed,
		})
	}

	return result, nil
}

func (s *PosService) ExportConfirmedTransactionsCSV(ctx context.Context, vendorID uint, posID uint) (string, error) {
	if ctx == nil {
		ctx = context.Background()
	}

	transactions, err := s.repo.FindTransactionsByPosID(ctx, vendorID, posID)
	if err != nil {
		return "", err
	}

	var builder strings.Builder
	builder.WriteString("Koinly Date,Amount,Currency,Label,TxHash")

	rows := 0
	for _, transaction := range transactions {
		if !transaction.Confirmed {
			continue
		}

		for _, sub := range transaction.SubTransactions {
			builder.WriteByte('\n')
			builder.WriteString(formatExportRow(sub))
			rows++
		}
	}

	if rows == 0 {
		return "", ErrNoConfirmedTransactions
	}

	return builder.String(), nil
}

func formatExportRow(sub *models.SubTransaction) string {
	date := sub.Timestamp.UTC()
	dateStr := fmt.Sprintf("%04d-%02d-%02d 00:00 UTC", date.Year(), date.Month(), date.Day())
	amount := formatAtomicAmountTwoDecimals(sub.Amount)

	return fmt.Sprintf("%s,%s,XMR,income,%s", dateStr, amount, sub.TxHash)
}

func formatAtomicAmountTwoDecimals(amount int64) string {
	integer := amount / moneroAtomicUnitsPerXMR
	remainder := amount % moneroAtomicUnitsPerXMR

	if remainder < 0 {
		remainder += moneroAtomicUnitsPerXMR
		integer--
	}

	centsDivisor := moneroAtomicUnitsPerXMR / 100
	roundingOffset := centsDivisor / 2

	decimals := (remainder + roundingOffset) / centsDivisor
	if decimals == 100 {
		integer++
		decimals = 0
	}

	return fmt.Sprintf("%d.%02d", integer, decimals)
}
