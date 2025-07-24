package pos

import (
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type PosRepository interface {
	FindTransactionByID(id uint) (*models.Transaction, error)
	CreateTransaction(*models.Transaction) (*models.Transaction, error)
	UpdateTransaction(*models.Transaction) (*models.Transaction, error)
}

type posRepository struct {
	db *gorm.DB
}

func NewPosRepository(db *gorm.DB) PosRepository {
	return &posRepository{db: db}
}

func (r *posRepository) FindTransactionByID(
	id uint,
) (*models.Transaction, error) {
	var transaction models.Transaction
	if err := r.db.Preload("SubTransactions").First(&transaction, id).Error; err != nil {
		return nil, err
	}
	return &transaction, nil
}

func (r *posRepository) CreateTransaction(
	transaction *models.Transaction,
) (*models.Transaction, error) {
	if err := r.db.Create(transaction).Error; err != nil {
		return nil, err
	}
	return transaction, nil
}

func (r *posRepository) UpdateTransaction(
	transaction *models.Transaction,
) (*models.Transaction, error) {
	if err := r.db.Save(transaction).Error; err != nil {
		return nil, err
	}
	return transaction, nil
}
