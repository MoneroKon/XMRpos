package pos

import (
	"context"
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

func NewPosService(repo PosRepository, cfg *config.Config, moneroPay *moneropay.MoneroPayAPIClient) *PosService {
	return &PosService{repo: repo, config: cfg, moneroPay: moneroPay}
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

	callbackUrl := s.config.MoneroPayCallbackURL + "receive/" + accessToken

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
