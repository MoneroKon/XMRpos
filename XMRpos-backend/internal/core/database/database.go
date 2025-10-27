package db

import (
	"context"
	"fmt"
	"strings"
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

	if err := dropLegacyUniqueNameIndexes(db); err != nil {
		return nil, err
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

	if err := ensureNameIndexes(db); err != nil {
		return nil, err
	}

	return db, nil
}

func dropLegacyUniqueNameIndexes(db *gorm.DB) error {
	if err := dropIndexesMatching(db,
		"pos",
		`indexdef LIKE '%"name"%' AND indexdef NOT LIKE '%"vendor_id"%'`,
	); err != nil {
		return fmt.Errorf("failed to drop legacy POS name index: %w", err)
	}

	if err := dropIndexesMatching(db,
		"vendors",
		`indexdef LIKE '%"name"%' AND indexdef NOT LIKE '%WHERE ("deleted_at" IS NULL)%'`,
	); err != nil {
		return fmt.Errorf("failed to drop legacy vendor name index: %w", err)
	}

	return nil
}

func dropIndexesMatching(db *gorm.DB, table string, condition string) error {
	type indexRow struct {
		IndexName string
	}

	query := fmt.Sprintf(`
		SELECT indexname
		FROM pg_indexes
		WHERE schemaname = current_schema()
		  AND tablename = '%s'
		  AND indexdef ILIKE 'CREATE UNIQUE INDEX%%'
		  AND %s
	`, table, condition)

	var rows []indexRow
	if err := db.Raw(query).Scan(&rows).Error; err != nil {
		return err
	}

	for _, row := range rows {
		if row.IndexName == "" {
			continue
		}
		stmt := fmt.Sprintf(`DROP INDEX IF EXISTS %s`, quoteIdentifier(row.IndexName))
		if err := db.Exec(stmt).Error; err != nil {
			return err
		}
	}

	return nil
}

func ensureNameIndexes(db *gorm.DB) error {
	if err := db.Migrator().CreateIndex(&models.Pos{}, "idx_pos_vendor_id_name"); err != nil {
		return fmt.Errorf("failed to ensure POS unique index: %w", err)
	}
	if err := db.Migrator().CreateIndex(&models.Vendor{}, "idx_vendor_name"); err != nil {
		return fmt.Errorf("failed to ensure vendor unique index: %w", err)
	}
	return nil
}

func quoteIdentifier(name string) string {
	return `"` + strings.ReplaceAll(name, `"`, `""`) + `"`
}
