package auth

import (
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type AuthRepository interface {
	FindPOSByVendorIDAndName(vendorID uint, name string) (*models.POS, error)
	FindVendorByName(name string) (*models.Vendor, error)
	UpdatePasswordHash(deviceID uint, newPassword string) error
}

type authRepository struct {
	db *gorm.DB
}

func NewAuthRepository(db *gorm.DB) AuthRepository {
	return &authRepository{db: db}
}

func (r *authRepository) FindPOSByVendorIDAndName(vendorID uint, name string) (*models.POS, error) {
	var pos models.POS
	if err := r.db.Where("vendor_id = ? AND name = ?", vendorID, name).First(&pos).Error; err != nil {
		return nil, err
	}
	return &pos, nil
}

func (r *authRepository) FindVendorByName(name string) (*models.Vendor, error) {
	var vendor models.Vendor
	if err := r.db.Where("name = ?", name).First(&vendor).Error; err != nil {
		return nil, err
	}
	return &vendor, nil
}

func (r *authRepository) UpdatePasswordHash(vendorID uint, newPasswordHash string) error {
	return r.db.Model(&models.Vendor{}).Where("ID = ?", vendorID).Update("password_hash", newPasswordHash).Error
}
