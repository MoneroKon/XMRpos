package admin

import (
	"time"
	"context"

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

func (s *AdminService) CreateInvite(ctx context.Context, validUntil time.Time, forcedName *string) (inviteCode string, err error) {
	if ctx == nil {
		ctx = context.Background()
	}

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

	_, err = s.repo.CreateInvite(ctx, invite)
	if err != nil {
		return "", err
	}

	return inviteCode, nil
}
