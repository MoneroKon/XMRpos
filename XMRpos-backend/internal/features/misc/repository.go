package misc

import (
	"context"

	"gorm.io/gorm"
)

type MiscRepository interface {
	GetPostgresqlHealth(ctx context.Context) (bool, error)
}

type miscRepository struct {
	db *gorm.DB
}

func NewMiscRepository(db *gorm.DB) MiscRepository {
	return &miscRepository{db: db}
}

func (r *miscRepository) GetPostgresqlHealth(ctx context.Context) (bool, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	var result int
	if err := r.db.WithContext(ctx).Raw("SELECT 1").Scan(&result).Error; err != nil {
		return false, err
	}
	return result == 1, nil
}
