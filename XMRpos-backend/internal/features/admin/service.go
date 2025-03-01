package admin

import (
	"time"

	gonanoid "github.com/matoous/go-nanoid/v2"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
)

type AdminService struct {
	repo   AdminRepository
	config *config.Config
}

func NewAdminService(repo AdminRepository, cfg *config.Config) *AdminService {
	return &AdminService{repo: repo, config: cfg}
}

func (s *AdminService) CreateInvite(validUntil time.Time, forcedName *string) (inviteCode string, err error) {
	inviteCode, err = gonanoid.New()

	if err != nil {
		return "", err
	}

	invite := &models.Invite{
		Used:       false,
		InviteCode: inviteCode,
		ForcedName: forcedName,
		ValidUntil: validUntil,
	}

	_, err = s.repo.CreateInvite(invite)
	if err != nil {
		return "", err
	}

	return inviteCode, nil
}
