package auth

import (
	"context"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type AuthRepository interface {
	FindPosByVendorIDAndName(ctx context.Context, vendorID uint, name string) (*models.Pos, error)
	FindVendorByName(ctx context.Context, name string) (*models.Vendor, error)
	FindVendorByID(ctx context.Context, id uint) (*models.Vendor, error)
	FindPosByID(ctx context.Context, id uint) (*models.Pos, error)
	UpdateVendorPasswordHash(ctx context.Context, vendorID uint, newPasswordHash string) (uint32, error)
	UpdatePosPasswordHash(ctx context.Context, posID uint, newPasswordHash string) (uint32, error)
}

type authRepository struct {
	db *gorm.DB
}

func NewAuthRepository(db *gorm.DB) AuthRepository {
	return &authRepository{db: db}
}

func (r *authRepository) FindPosByVendorIDAndName(ctx context.Context, vendorID uint, name string) (*models.Pos, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var pos models.Pos
	if err := r.db.WithContext(ctx).Where("vendor_id = ? AND name = ?", vendorID, name).First(&pos).Error; err != nil {
		return nil, err
	}
	return &pos, nil
}

func (r *authRepository) FindVendorByName(ctx context.Context, name string) (*models.Vendor, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var vendor models.Vendor
	if err := r.db.WithContext(ctx).Where("name = ?", name).First(&vendor).Error; err != nil {
		return nil, err
	}
	return &vendor, nil
}

func (r *authRepository) FindVendorByID(ctx context.Context, id uint) (*models.Vendor, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var vendor models.Vendor
	if err := r.db.WithContext(ctx).Where("id = ?", id).First(&vendor).Error; err != nil {
		return nil, err
	}
	return &vendor, nil
}

func (r *authRepository) FindPosByID(ctx context.Context, id uint) (*models.Pos, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var pos models.Pos
	if err := r.db.WithContext(ctx).Where("id = ?", id).First(&pos).Error; err != nil {
		return nil, err
	}
	return &pos, nil
}

func (r *authRepository) UpdateVendorPasswordHash(ctx context.Context, vendorID uint, newPasswordHash string) (passwordVersion uint32, err error) {
	if ctx == nil {
		ctx = context.Background()
	}
	// Update password and increment password_version
	err = r.db.WithContext(ctx).Model(&models.Vendor{}).
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
	if err := r.db.WithContext(ctx).Select("password_version").Where("id = ?", vendorID).First(&vendor).Error; err != nil {
		return 0, err
	}
	return vendor.PasswordVersion, nil
}

func (r *authRepository) UpdatePosPasswordHash(ctx context.Context, posID uint, newPasswordHash string) (passwordVersion uint32, err error) {
	if ctx == nil {
		ctx = context.Background()
	}
	// Update password and increment password_version
	err = r.db.WithContext(ctx).Model(&models.Pos{}).
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
	if err := r.db.WithContext(ctx).Select("password_version").Where("id = ?", posID).First(&pos).Error; err != nil {
		return 0, err
	}
	return pos.PasswordVersion, nil
}
