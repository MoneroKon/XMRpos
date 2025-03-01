package vendor

import (
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type VendorRepository interface {
	VendorByNameExists(name string) (bool, error)
	FindInviteByCode(inviteCode string) (*models.Invite, error)
	CreateVendor(vendor *models.Vendor) error
	SetInviteToUsed(inviteID uint) error
	GetVendorByID(vendorID uint) (*models.Vendor, error)
	DeleteVendor(vendorID uint) error
	DeleteAllTransactionsForVendor(vendorID uint) error
	DeleteAllPOSForVendor(vendorID uint) error
	POSByNameExistsForVendor(name string, vendorID uint) (bool, error)
	CreatePOS(pos *models.POS) error
}

type vendorRepository struct {
	db *gorm.DB
}

func NewVendorRepository(db *gorm.DB) VendorRepository {
	return &vendorRepository{db: db}
}

func (r *vendorRepository) VendorByNameExists(name string) (bool, error) {
	var count int64
	if err := r.db.Model(&models.Vendor{}).Where("name = ?", name).Count(&count).Error; err != nil {
		return false, err
	}
	return count > 0, nil
}

func (r *vendorRepository) FindInviteByCode(inviteCode string) (*models.Invite, error) {
	var invite models.Invite
	if err := r.db.Where("invite_code = ?", inviteCode).First(&invite).Error; err != nil {
		return nil, err
	}
	return &invite, nil
}

func (r *vendorRepository) CreateVendor(vendor *models.Vendor) error {
	if err := r.db.Create(vendor).Error; err != nil {
		return err
	}
	return nil
}

func (r *vendorRepository) SetInviteToUsed(inviteID uint) error {
	return r.db.Model(&models.Invite{}).Where("id = ?", inviteID).Update("used", true).Error
}

func (r *vendorRepository) GetVendorByID(vendorID uint) (*models.Vendor, error) {
	var vendor models.Vendor
	if err := r.db.First(&vendor, vendorID).Error; err != nil {
		return nil, err
	}
	return &vendor, nil
}

func (r *vendorRepository) DeleteVendor(vendorID uint) error {
	return r.db.Delete(&models.Vendor{}, vendorID).Error
}

func (r *vendorRepository) DeleteAllTransactionsForVendor(vendorID uint) error {
	return r.db.Where("vendor_id = ?", vendorID).Delete(&models.Transaction{}).Error
}

func (r *vendorRepository) DeleteAllPOSForVendor(vendorID uint) error {
	return r.db.Where("vendor_id = ?", vendorID).Delete(&models.POS{}).Error
}

func (r *vendorRepository) POSByNameExistsForVendor(name string, vendorID uint) (bool, error) {
	var count int64
	if err := r.db.Model(&models.POS{}).Where("name = ? AND vendor_id = ?", name, vendorID).Count(&count).Error; err != nil {
		return false, err
	}
	return count > 0, nil
}

func (r *vendorRepository) CreatePOS(pos *models.POS) error {
	if err := r.db.Create(pos).Error; err != nil {
		return err
	}
	return nil
}
