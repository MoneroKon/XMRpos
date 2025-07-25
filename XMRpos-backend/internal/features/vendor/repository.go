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
	DeleteAllPosForVendor(vendorID uint) error
	PosByNameExistsForVendor(name string, vendorID uint) (bool, error)
	CreatePos(pos *models.Pos) error
	GetBalance(vendorID uint) (int64, error)
	GetActiveTransferByVendorID(vendorID uint) (*models.Transfer, error)
	GetAllTransferableTransactions(vendorID uint) ([]*models.Transaction, error)
	CreateTransfer(transfer *models.Transfer) error
	GetTransfersToComplete(limit int) ([]*models.Transfer, error)
	MarkTransactionsTransferred(tx *gorm.DB, transferID uint, transactionIDs []uint) error
	MarkTransferCompleted(tx *gorm.DB, transferID uint, AmountTransferred int64, txHash string) error
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

func (r *vendorRepository) DeleteAllPosForVendor(vendorID uint) error {
	return r.db.Where("vendor_id = ?", vendorID).Delete(&models.Pos{}).Error
}

func (r *vendorRepository) PosByNameExistsForVendor(name string, vendorID uint) (bool, error) {
	var count int64
	if err := r.db.Model(&models.Pos{}).Where("name = ? AND vendor_id = ?", name, vendorID).Count(&count).Error; err != nil {
		return false, err
	}
	return count > 0, nil
}

func (r *vendorRepository) CreatePos(pos *models.Pos) error {
	if err := r.db.Create(pos).Error; err != nil {
		return err
	}
	return nil
}

func (r *vendorRepository) GetBalance(vendorID uint) (int64, error) {
	var balance int64
	err := r.db.Model(&models.Transaction{}).
		Where("vendor_id = ? AND confirmed = ? AND transferred = ?", vendorID, true, false).
		Select("COALESCE(SUM(amount), 0)").
		Scan(&balance).Error
	if err != nil {
		return 0, err
	}
	return balance, nil
}

func (r *vendorRepository) GetActiveTransferByVendorID(vendorID uint) (*models.Transfer, error) {
	var transfer models.Transfer
	if err := r.db.Where("vendor_id = ? AND completed = ?", vendorID, false).First(&transfer).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, nil // No transfer found
		}
		return nil, err
	}
	return &transfer, nil
}

func (r *vendorRepository) GetAllTransferableTransactions(vendorID uint) ([]*models.Transaction, error) {
	var transactions []*models.Transaction
	if err := r.db.Where("vendor_id = ? AND confirmed = ? AND transferred = ?", vendorID, true, false).Find(&transactions).Error; err != nil {
		return nil, err
	}
	return transactions, nil
}

func (r *vendorRepository) CreateTransfer(transfer *models.Transfer) error {
	if err := r.db.Create(transfer).Error; err != nil {
		return err
	}
	return nil
}

func (r *vendorRepository) GetTransfersToComplete(limit int) ([]*models.Transfer, error) {
	var transfers []*models.Transfer
	if err := r.db.Preload("Transactions").Where("completed = ?", false).Order("created_at ASC").Limit(limit).Find(&transfers).Error; err != nil {
		return nil, err
	}
	return transfers, nil
}

func (r *vendorRepository) MarkTransactionsTransferred(tx *gorm.DB, transferID uint, transactionIDs []uint) error {
	return tx.Model(&models.Transaction{}).
		Where("id IN ?", transactionIDs).
		Updates(map[string]interface{}{
			"transferred": true,
			"transfer_id": transferID,
		}).Error
}

func (r *vendorRepository) MarkTransferCompleted(tx *gorm.DB, transferID uint, amountTransferred int64, txHash string) error {
	return tx.Model(&models.Transfer{}).
		Where("id = ?", transferID).
		Updates(map[string]interface{}{
			"completed":          true,
			"tx_hash":            txHash,
			"amount_transferred": amountTransferred,
		}).Error
}
