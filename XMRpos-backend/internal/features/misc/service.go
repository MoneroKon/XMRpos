package misc

import (
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/thirdparty/moneropay"
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
func (s *MiscService) GetHealth() (healthResponse HealthResponse) {
	healthResponse = HealthResponse{}

	// get the status of the MoneroPay service
	moneroPayStatus, moneroPayErr := s.moneroPay.GetHealth()

	if moneroPayErr != nil {
		healthResponse.Services.MoneroPay.Status = 503
	}
	healthResponse.Services.MoneroPay = *moneroPayStatus

	// get the status of the PostgreSQL service
	postgresqlStatus, err := s.repo.GetPostgresqlHealth()
	if err != nil {
		healthResponse.Services.Postgresql = false
	} else {
		healthResponse.Services.Postgresql = postgresqlStatus
	}

	if (moneroPayErr != nil) || (err != nil) || (healthResponse.Services.MoneroPay.Status != 200) || (!healthResponse.Services.Postgresql) {
		healthResponse.Status = 503
	} else {
		healthResponse.Status = 200
	}

	return healthResponse
}
