package pos

import (
	"context"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type PosRepository interface {
	FindTransactionByID(ctx context.Context, id uint) (*models.Transaction, error)
	CreateTransaction(ctx context.Context, transaction *models.Transaction) (*models.Transaction, error)
	UpdateTransaction(ctx context.Context, transaction *models.Transaction) (*models.Transaction, error)
}

type posRepository struct {
	db *gorm.DB
}

func NewPosRepository(db *gorm.DB) PosRepository {
	return &posRepository{db: db}
}

func (r *posRepository) FindTransactionByID(ctx context.Context, id uint) (*models.Transaction, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var transaction models.Transaction
	if err := r.db.WithContext(ctx).Preload("SubTransactions").First(&transaction, id).Error; err != nil {
		return nil, err
	}
	return &transaction, nil
}

func (r *posRepository) CreateTransaction(ctx context.Context, transaction *models.Transaction) (*models.Transaction, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	if err := r.db.WithContext(ctx).Create(transaction).Error; err != nil {
		return nil, err
	}
	return transaction, nil
}

func (r *posRepository) UpdateTransaction(ctx context.Context, transaction *models.Transaction) (*models.Transaction, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	if err := r.db.WithContext(ctx).Save(transaction).Error; err != nil {
		return nil, err
	}
	return transaction, nil
}
