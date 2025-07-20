package pos

import (
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/gorm"
)

type PosRepository interface {
	CreateTransaction(*models.Transaction) (*models.Transaction, error)
}

type posRepository struct {
	db *gorm.DB
}

func NewPosRepository(db *gorm.DB) PosRepository {
	return &posRepository{db: db}
}

func (r *posRepository) CreateTransaction(
	transaction *models.Transaction,
) (*models.Transaction, error) {
	if err := r.db.Create(transaction).Error; err != nil {
		return nil, err
	}
	return transaction, nil
}
