package auth

import (
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type AuthRepository interface {
	FindPosByVendorIDAndName(vendorID uint, name string) (*models.Pos, error)
	FindVendorByName(name string) (*models.Vendor, error)
	FindVendorByID(id uint) (*models.Vendor, error)
	FindPosByID(id uint) (*models.Pos, error)
	UpdateVendorPasswordHash(vendorID uint, newPasswordHash string) (uint32, error)
	UpdatePosPasswordHash(posID uint, newPasswordHash string) (uint32, error)
}

type authRepository struct {
	db *gorm.DB
}

func NewAuthRepository(db *gorm.DB) AuthRepository {
	return &authRepository{db: db}
}

func (r *authRepository) FindPosByVendorIDAndName(vendorID uint, name string) (*models.Pos, error) {
	var pos models.Pos
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

func (r *authRepository) FindVendorByID(id uint) (*models.Vendor, error) {
	var vendor models.Vendor
	if err := r.db.Where("id = ?", id).First(&vendor).Error; err != nil {
		return nil, err
	}
	return &vendor, nil
}

func (r *authRepository) FindPosByID(id uint) (*models.Pos, error) {
	var pos models.Pos
	if err := r.db.Where("id = ?", id).First(&pos).Error; err != nil {
		return nil, err
	}
	return &pos, nil
}

func (r *authRepository) UpdateVendorPasswordHash(vendorID uint, newPasswordHash string) (passwordVersion uint32, err error) {
	// Update password and increment password_version
	err = r.db.Model(&models.Vendor{}).
		Where("id = ?", vendorID).
		Updates(map[string]interface{}{
			"password_hash":    newPasswordHash,
			"password_version": gorm.Expr("password_version + 1"),
		}).Error
	if err != nil {
		return 0, err
	}

	// Fetch the new password_version
	var vendor models.Vendor
	if err := r.db.Select("password_version").Where("id = ?", vendorID).First(&vendor).Error; err != nil {
		return 0, err
	}
	return vendor.PasswordVersion, nil
}

func (r *authRepository) UpdatePosPasswordHash(posID uint, newPasswordHash string) (passwordVersion uint32, err error) {
	// Update password and increment password_version
	err = r.db.Model(&models.Pos{}).
		Where("id = ?", posID).
		Updates(map[string]interface{}{
			"password_hash":    newPasswordHash,
			"password_version": gorm.Expr("password_version + 1"),
		}).Error
	if err != nil {
		return 0, err
	}

	// Fetch the new password_version
	var pos models.Pos
	if err := r.db.Select("password_version").Where("id = ?", posID).First(&pos).Error; err != nil {
		return 0, err
	}
	return pos.PasswordVersion, nil
}
