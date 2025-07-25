package db

import (
	"fmt"
	/* "log" */

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

func NewPostgresClient(cfg *config.Config) (*gorm.DB, error) {
	// Connect to the default database to check for the existence of the target database
	/* defaultDSN := fmt.Sprintf("host=%s user=%s password=%s dbname=postgres sslmode=disable",
		cfg.DBHost, cfg.DBUser, cfg.DBPassword)

	defaultDB, err := gorm.Open(postgres.Open(defaultDSN), &gorm.Config{})
	if err != nil {
		return nil, fmt.Errorf("failed to connect to default database: %w", err)
	}
	sqlDB, err := defaultDB.DB()
	if err != nil {
		return nil, fmt.Errorf("failed to get underlying database: %w", err)
	}
	defer sqlDB.Close()

	// Check if the database exists
	var exists bool
	defaultDB.Raw("SELECT EXISTS(SELECT 1 FROM pg_database WHERE datname = ?)", cfg.DBName).Row().Scan(&exists)
	if !exists {
		// Create the database if it does not exist
		if err := defaultDB.Exec(fmt.Sprintf("CREATE DATABASE %s", cfg.DBName)).Error; err != nil {
			return nil, fmt.Errorf("failed to create database: %w", err)
		}
		log.Printf("Database %s created successfully", cfg.DBName)
	}

	// Check if the user exists
	var userExists bool
	defaultDB.Raw("SELECT EXISTS(SELECT 1 FROM pg_roles WHERE rolname = ?)", cfg.DBUser).Row().Scan(&userExists)
	if !userExists {
		// Create the user if it does not exist
		if err := defaultDB.Exec(fmt.Sprintf("CREATE USER %s WITH PASSWORD '%s'", cfg.DBUser, cfg.DBPassword)).Error; err != nil {
			return nil, fmt.Errorf("failed to create user: %w", err)
		}
		log.Printf("User %s created successfully", cfg.DBUser)

		// Grant all privileges on the database to the user
		if err := defaultDB.Exec(fmt.Sprintf("GRANT ALL PRIVILEGES ON DATABASE %s TO %s", cfg.DBName, cfg.DBUser)).Error; err != nil {
			return nil, fmt.Errorf("failed to grant privileges: %w", err)
		}
		log.Printf("Granted all privileges on database %s to user %s", cfg.DBName, cfg.DBUser)
	} */

	// Connect to the target database
	dsn := fmt.Sprintf("host=%s user=%s password=%s dbname=%s sslmode=disable",
		cfg.DBHost, cfg.DBUser, cfg.DBPassword, cfg.DBName)

	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{})
	if err != nil {
		return nil, fmt.Errorf("failed to connect to target database: %w", err)
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
