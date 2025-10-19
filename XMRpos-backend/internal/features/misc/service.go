package misc

import (
	"context"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/thirdparty/moneropay"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
)

type MiscService struct {
	repo      MiscRepository
	config    *config.Config
	moneroPay *moneropay.MoneroPayAPIClient
}

func NewMiscService(repo MiscRepository, cfg *config.Config, moneroPay *moneropay.MoneroPayAPIClient) *MiscService {
	return &MiscService{repo: repo, config: cfg, moneroPay: moneroPay}
}

// Check if the vendor and POS are authorized for the transaction
func (s *MiscService) GetHealth(ctx context.Context) HealthResponse {
	h := HealthResponse{}

	// Check MoneroPay service health
	mp, mpErr := s.moneroPay.GetHealth(ctx)
	if mpErr != nil {
		h.Services.MoneroPay.Status = 503
	} else {
		h.Services.MoneroPay = *mp
	}

	// Check PostgreSQL service health
	postgresqlStatus, pgErr := s.repo.GetPostgresqlHealth(ctx)
	h.Services.Postgresql = postgresqlStatus && pgErr == nil

	// Set overall status
	if h.Services.Postgresql && h.Services.MoneroPay.Status == 200 {
		h.Status = 200
	} else {
		h.Status = 503
	}

	return h
}
