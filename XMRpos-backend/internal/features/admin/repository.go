package admin

import (
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type AdminRepository interface {
	CreateInvite(invite *models.Invite) (*models.Invite, error)
}

type adminRepository struct {
	db *gorm.DB
}

func NewAdminRepository(db *gorm.DB) AdminRepository {
	return &adminRepository{db: db}
}

func (r *adminRepository) CreateInvite(invite *models.Invite) (*models.Invite, error) {
	if err := r.db.Create(invite).Error; err != nil {
		return nil, err
	}
	return invite, nil
}
