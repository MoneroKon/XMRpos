package admin

import (
	"context"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type AdminRepository interface {
	CreateInvite(ctx context.Context, invite *models.Invite) (*models.Invite, error)
}

type adminRepository struct {
	db *gorm.DB
}

func NewAdminRepository(db *gorm.DB) AdminRepository {
	return &adminRepository{db: db}
}

func (r *adminRepository) CreateInvite(ctx context.Context, invite *models.Invite) (*models.Invite, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	if err := r.db.WithContext(ctx).Create(invite).Error; err != nil {
		return nil, err
	}
	return invite, nil
}
