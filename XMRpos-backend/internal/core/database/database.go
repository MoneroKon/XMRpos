package db

import (
	"context"
	"fmt"
	"time"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

func NewPostgresClient(cfg *config.Config) (*gorm.DB, error) {
	// Apply server-side timeouts across pooled connections via options
	dsn := fmt.Sprintf("host=%s user=%s password=%s dbname=%s port=%s sslmode=disable connect_timeout=5 "+
        "options='-c statement_timeout=15s -c idle_in_transaction_session_timeout=15s'",
        cfg.DBHost, cfg.DBUser, cfg.DBPassword, cfg.DBName, cfg.DBPort)

	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{})
	if err != nil {
		return nil, fmt.Errorf("failed to connect to target database: %w", err)
	}

	sqlDB, err := db.DB()
	if err != nil {
		return nil, fmt.Errorf("failed to get underlying database: %w", err)
	}
	sqlDB.SetMaxOpenConns(20)
	sqlDB.SetMaxIdleConns(10)
	sqlDB.SetConnMaxLifetime(30 * time.Minute)
	sqlDB.SetConnMaxIdleTime(5 * time.Minute)

	// Fail fast if DB is unhealthy with context
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	if err := sqlDB.PingContext(ctx); err != nil {
		return nil, fmt.Errorf("database not reachable: %w", err)
	}

	// Auto-migrate schemas
	err = db.AutoMigrate(
		&models.Invite{},
		&models.Transaction{},
		&models.SubTransaction{},
		&models.Pos{},
		&models.Vendor{},
		&models.Transfer{},
	)
	if err != nil {
		return nil, err
	}

	return db, nil
}
