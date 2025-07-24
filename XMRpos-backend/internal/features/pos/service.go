package pos

import (
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

func (s *PosService) CreateTransaction(vendorID uint, posID uint, amount int64, description *string, amountInCurrency float64, currency string, requiredConfirmations int64) (address string, err error) {

	transaction := &models.Transaction{
		VendorID:              vendorID,
		PosID:                 posID,
		Amount:                amount,
		RequiredConfirmations: requiredConfirmations,
		Currency:              currency,
		AmountInCurrency:      amountInCurrency,
		Description:           description,
	}

	transactionDB, err := s.repo.CreateTransaction(transaction)
	if err != nil {
		return "", err
	}

	// Create a jwt token for the transaction which contains the transaction ID
	moneroPayTokenJWT := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"transaction_id": transactionDB.ID,
		"exp":            time.Now().Add(time.Hour * 6).Unix(),
	})

	accessToken, err := moneroPayTokenJWT.SignedString([]byte(s.config.JWTMoneroPaySecret))
	if err != nil {
		return "", err
	}

	callbackUrl := s.config.MoneroPayCallbackURL + "receive/" + accessToken

	req := &moneropay.ReceiveRequest{
		Amount:      amount,
		Description: *description,
		CallbackUrl: callbackUrl,
	}

	resp, err := s.moneroPay.PostReceive(req)
	if err != nil {
		return "", err
	}

	// Update the transaction with the subaddress received from MoneroPay
	transactionDB.SubAddress = &resp.Address
	if _, err := s.repo.UpdateTransaction(transactionDB); err != nil {
		return "", err
	}

	return resp.Address, nil
}

// Check if the vendor and POS are authorized for the transaction
func (s *PosService) IsAuthorizedForTransaction(vendorID uint, posID uint, transactionID uint) bool {
	// Get the transaction by ID
	transaction, err := s.repo.FindTransactionByID(transactionID)
	if err != nil {
		return false
	}

	if transaction.VendorID != vendorID || transaction.PosID != posID {
		return false
	}
	return true
}
