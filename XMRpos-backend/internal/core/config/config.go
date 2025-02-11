package config

import (
	"fmt"
	"os"

	"github.com/joho/godotenv"
)

type Config struct {
	// Admin Configuration
	AdminName     string
	AdminPassword string

	// Database Configuration
	DBHost     string
	DBUser     string
	DBPassword string
	DBName     string
	DBPort     string

	// JWT Configuration
	JWTSecret        string
	JWTRefreshSecret string

	// MoneroPay API Configuration
	MoneroPayBaseURL string
}

func LoadConfig() (*Config, error) {
	if err := godotenv.Load(); err != nil {
		return nil, fmt.Errorf("error loading .env file: %w", err)
	}

	config := &Config{
		// Admin Configuration
		AdminName:     os.Getenv("ADMIN_NAME"),
		AdminPassword: os.Getenv("ADMIN_PASSWORD"),

		// Database Configuration
		DBHost:     os.Getenv("DB_HOST"),
		DBUser:     os.Getenv("DB_USER"),
		DBPassword: os.Getenv("DB_PASSWORD"),
		DBName:     os.Getenv("DB_NAME"),
		DBPort:     os.Getenv("DB_PORT"),

		// JWT Configuration
		JWTSecret:        os.Getenv("JWT_SECRET"),
		JWTRefreshSecret: os.Getenv("JWT_REFRESH_SECRET"),

		// MoneroPay API Configuration
		MoneroPayBaseURL: os.Getenv("MONEROPAY_BASE_URL"),
	}

	// Validate required fields
	if config.AdminName == "" ||
		config.AdminPassword == "" ||
		config.DBHost == "" ||
		config.DBUser == "" ||
		config.DBPassword == "" ||
		config.DBName == "" ||
		config.DBPort == "" ||
		config.JWTSecret == "" ||
		config.JWTRefreshSecret == "" ||
		config.MoneroPayBaseURL == "" {
		return nil, fmt.Errorf("missing required environment variables")
	}

	return config, nil
}
