package callback

import (
	"time"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type CallbackRepository interface {
	FindTransactionByID(id uint) (*models.Transaction, error)
	FindUnconfirmedTransactions() ([]*models.Transaction, error)
	UpdateTransaction(transaction *models.Transaction) (*models.Transaction, error)
	UpdateSubTransaction(subTx *models.SubTransaction) (*models.SubTransaction, error)
	CreateSubTransaction(subTx *models.SubTransaction) (*models.SubTransaction, error)
}

type callbackRepository struct {
	db *gorm.DB
}

func NewCallbackRepository(db *gorm.DB) CallbackRepository {
	return &callbackRepository{db: db}
}

func (r *callbackRepository) FindTransactionByID(
	id uint,
) (*models.Transaction, error) {
	var transaction models.Transaction
	if err := r.db.Preload("SubTransactions").First(&transaction, id).Error; err != nil {
		return nil, err
	}
	return &transaction, nil
}

func (r *callbackRepository) FindUnconfirmedTransactions() ([]*models.Transaction, error) {
	var transactions []*models.Transaction
	if err := r.db.
		Preload("SubTransactions").
		Where("confirmed = ? AND created_at < ?", false, time.Now().Add(-time.Hour*6)).
		Find(&transactions).Error; err != nil {
		return nil, err
	}
	return transactions, nil
}

// Update only the main transaction fields
func (r *callbackRepository) UpdateTransaction(
	transaction *models.Transaction,
) (*models.Transaction, error) {
	if err := r.db.Model(&models.Transaction{}).Where("id = ?", transaction.ID).Updates(transaction).Error; err != nil {
		return nil, err
	}
	return transaction, nil
}

// Update an existing subtransaction (by ID)
func (r *callbackRepository) UpdateSubTransaction(
	subTx *models.SubTransaction,
) (*models.SubTransaction, error) {
	if err := r.db.Model(&models.SubTransaction{}).Where("id = ?", subTx.ID).Updates(subTx).Error; err != nil {
		return nil, err
	}
	return subTx, nil
}

// Create a new subtransaction
func (r *callbackRepository) CreateSubTransaction(
	subTx *models.SubTransaction,
) (*models.SubTransaction, error) {
	if err := r.db.Create(subTx).Error; err != nil {
		return nil, err
	}
	return subTx, nil
}
