package vendor

import (
	"context"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type VendorRepository interface {
	VendorByNameExists(ctx context.Context, name string) (bool, error)
	FindInviteByCode(ctx context.Context, inviteCode string) (*models.Invite, error)
	CreateVendor(ctx context.Context, vendor *models.Vendor) error
	SetInviteToUsed(ctx context.Context, inviteID uint) error
	GetVendorByID(ctx context.Context, vendorID uint) (*models.Vendor, error)
	DeleteVendor(ctx context.Context, vendorID uint) error
	DeleteAllTransactionsForVendor(ctx context.Context, vendorID uint) error
	DeleteAllPosForVendor(ctx context.Context, vendorID uint) error
	PosByNameExistsForVendor(ctx context.Context, name string, vendorID uint) (bool, error)
	CreatePos(ctx context.Context, pos *models.Pos) error
	GetBalance(ctx context.Context, vendorID uint) (int64, error)
	GetActiveTransferByVendorID(ctx context.Context, vendorID uint) (*models.Transfer, error)
	GetAllTransferableTransactions(ctx context.Context, vendorID uint) ([]*models.Transaction, error)
	CreateTransfer(ctx context.Context, transfer *models.Transfer) error
	GetTransfersToComplete(ctx context.Context, limit int) ([]*models.Transfer, error)
	MarkTransactionsTransferred(ctx context.Context, tx *gorm.DB, transferID uint, transactionIDs []uint) error
	MarkTransferCompleted(ctx context.Context, tx *gorm.DB, transferID uint, AmountTransferred int64, txHash string) error
}

type vendorRepository struct {
	db *gorm.DB
}

func NewVendorRepository(db *gorm.DB) VendorRepository {
	return &vendorRepository{db: db}
}

func (r *vendorRepository) VendorByNameExists(ctx context.Context, name string) (bool, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var count int64
	if err := r.db.WithContext(ctx).Model(&models.Vendor{}).Where("name = ?", name).Count(&count).Error; err != nil {
		return false, err
	}
	return count > 0, nil
}

func (r *vendorRepository) FindInviteByCode(ctx context.Context, inviteCode string) (*models.Invite, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var invite models.Invite
	if err := r.db.WithContext(ctx).Where("invite_code = ?", inviteCode).First(&invite).Error; err != nil {
		return nil, err
	}
	return &invite, nil
}

func (r *vendorRepository) CreateVendor(ctx context.Context, vendor *models.Vendor) error {
	if ctx == nil {
		ctx = context.Background()
	}
	return r.db.WithContext(ctx).Create(vendor).Error
}

func (r *vendorRepository) SetInviteToUsed(ctx context.Context, inviteID uint) error {
	if ctx == nil {
		ctx = context.Background()
	}
	return r.db.WithContext(ctx).Model(&models.Invite{}).Where("id = ?", inviteID).Update("used", true).Error
}

func (r *vendorRepository) GetVendorByID(ctx context.Context, vendorID uint) (*models.Vendor, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var vendor models.Vendor
	if err := r.db.WithContext(ctx).First(&vendor, vendorID).Error; err != nil {
		return nil, err
	}
	return &vendor, nil
}

func (r *vendorRepository) DeleteVendor(ctx context.Context, vendorID uint) error {
	if ctx == nil {
		ctx = context.Background()
	}
	return r.db.WithContext(ctx).Delete(&models.Vendor{}, vendorID).Error
}

func (r *vendorRepository) DeleteAllTransactionsForVendor(ctx context.Context, vendorID uint) error {
	if ctx == nil {
		ctx = context.Background()
	}
	return r.db.WithContext(ctx).Where("vendor_id = ?", vendorID).Delete(&models.Transaction{}).Error
}

func (r *vendorRepository) DeleteAllPosForVendor(ctx context.Context, vendorID uint) error {
	if ctx == nil {
		ctx = context.Background()
	}
	return r.db.WithContext(ctx).Where("vendor_id = ?", vendorID).Delete(&models.Pos{}).Error
}

func (r *vendorRepository) PosByNameExistsForVendor(ctx context.Context, name string, vendorID uint) (bool, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var count int64
	if err := r.db.WithContext(ctx).Model(&models.Pos{}).
		Where("name = ? AND vendor_id = ?", name, vendorID).
		Count(&count).Error; err != nil {
		return false, err
	}
	return count > 0, nil
}

func (r *vendorRepository) CreatePos(ctx context.Context, pos *models.Pos) error {
	if ctx == nil {
		ctx = context.Background()
	}
	return r.db.WithContext(ctx).Create(pos).Error
}

func (r *vendorRepository) GetBalance(ctx context.Context, vendorID uint) (int64, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var balance int64
	err := r.db.WithContext(ctx).Model(&models.Transaction{}).
		Where("vendor_id = ? AND confirmed = ? AND transferred = ?", vendorID, true, false).
		Select("COALESCE(SUM(amount), 0)").
		Scan(&balance).Error
	if err != nil {
		return 0, err
	}
	return balance, nil
}

func (r *vendorRepository) GetActiveTransferByVendorID(ctx context.Context, vendorID uint) (*models.Transfer, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var transfer models.Transfer
	err := r.db.WithContext(ctx).Where("vendor_id = ? AND completed = ?", vendorID, false).First(&transfer).Error
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, nil // No transfer found
		}
		return nil, err
	}
	return &transfer, nil
}

func (r *vendorRepository) GetAllTransferableTransactions(ctx context.Context, vendorID uint) ([]*models.Transaction, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var transactions []*models.Transaction
	if err := r.db.WithContext(ctx).
		Where("vendor_id = ? AND confirmed = ? AND transferred = ?", vendorID, true, false).
		Find(&transactions).Error; err != nil {
		return nil, err
	}
	return transactions, nil
}

func (r *vendorRepository) CreateTransfer(ctx context.Context, transfer *models.Transfer) error {
	if ctx == nil {
		ctx = context.Background()
	}
	return r.db.WithContext(ctx).Create(transfer).Error
}

func (r *vendorRepository) GetTransfersToComplete(ctx context.Context, limit int) ([]*models.Transfer, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var transfers []*models.Transfer
	if err := r.db.WithContext(ctx).
		Preload("Transactions").
		Where("completed = ?", false).
		Order("created_at ASC").
		Limit(limit).
		Find(&transfers).Error; err != nil {
		return nil, err
	}
	return transfers, nil
}

func (r *vendorRepository) MarkTransactionsTransferred(ctx context.Context, tx *gorm.DB, transferID uint, transactionIDs []uint) error {
	if ctx == nil {
		ctx = context.Background()
	}
	return tx.WithContext(ctx).Model(&models.Transaction{}).
		Where("id IN ?", transactionIDs).
		Updates(map[string]interface{}{
			"transferred": true,
			"transfer_id": transferID,
		}).Error
}

func (r *vendorRepository) MarkTransferCompleted(ctx context.Context, tx *gorm.DB, transferID uint, AmountTransferred int64, txHash string) error {
	if ctx == nil {
		ctx = context.Background()
	}
	return tx.WithContext(ctx).Model(&models.Transfer{}).
		Where("id = ?", transferID).
		Updates(map[string]interface{}{
			"completed":          true,
			"tx_hash":            txHash,
			"amount_transferred": AmountTransferred,
		}).Error
}
