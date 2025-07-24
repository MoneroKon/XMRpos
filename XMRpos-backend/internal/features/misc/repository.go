package misc

import (
	"gorm.io/gorm"
)

type MiscRepository interface {
	GetPostgresqlHealth() (bool, error)
}

type miscRepository struct {
	db *gorm.DB
}

func NewMiscRepository(db *gorm.DB) MiscRepository {
	return &miscRepository{db: db}
}

func (r *miscRepository) GetPostgresqlHealth() (bool, error) {
	var result int
	if err := r.db.Raw("SELECT 1").Scan(&result).Error; err != nil {
		return false, err
	}
	return result == 1, nil
}
