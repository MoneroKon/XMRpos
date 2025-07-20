package pos

import (
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

func (s *PosService) CreateTransaction(vendorID uint, posID uint, amount int64, description *string, amountInCurrency float64, currency string, requiredConfirmations int) (address string, err error) {

	req := &moneropay.ReceiveRequest{
		Amount:      amount,
		Description: *description,
	}

	resp, err := s.moneroPay.PostReceive(req)
	if err != nil {
		return "", err
	}

	transaction := &models.Transaction{
		VendorID:              vendorID,
		PosID:                 posID,
		Amount:                amount,
		RequiredConfirmations: requiredConfirmations,
		Currency:              currency,
		AmountInCurrency:      amountInCurrency,
		Description:           description,
		SubAddress:            resp.Address,
	}

	if _, err := s.repo.CreateTransaction(transaction); err != nil {
		return "", err
	}

	return resp.Address, nil
}
