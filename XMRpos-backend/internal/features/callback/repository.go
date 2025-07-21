package callback

import (
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type CallbackRepository interface {
	FindTransactionByID(id uint) (*models.Transaction, error)
	UpdateTransaction(transaction *models.Transaction) (*models.Transaction, error)
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
	if err := r.db.First(&transaction, id).Error; err != nil {
		return nil, err
	}
	return &transaction, nil
}

func (r *callbackRepository) UpdateTransaction(
	transaction *models.Transaction,
) (*models.Transaction, error) {
	if err := r.db.Save(transaction).Error; err != nil {
		return nil, err
	}
	if err := r.db.Model(transaction).Association("SubTransactions").Replace(transaction.SubTransactions); err != nil {
		return nil, err
	}
	return transaction, nil
}
