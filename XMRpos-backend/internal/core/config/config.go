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

	// Server Configuration
	Port string

	// Database Configuration
	DBHost     string
	DBUser     string
	DBPassword string
	DBName     string
	DBPort     string

	// JWT Configuration
	JWTSecret          string
	JWTRefreshSecret   string
	JWTMoneroPaySecret string

	// MoneroPay API Configuration
	MoneroPayBaseURL     string
	MoneroPayCallbackURL string

	// Monero Wallet RPC Configuration
	MoneroWalletRPCEndpoint string
	MoneroWalletRPCUsername string
	MoneroWalletRPCPassword string
}

func LoadConfig() (*Config, error) {
	if err := godotenv.Load(); err != nil {
		return nil, fmt.Errorf("error loading .env file: %w", err)
	}

	config := &Config{
		// Admin Configuration
		AdminName:     os.Getenv("ADMIN_NAME"),
		AdminPassword: os.Getenv("ADMIN_PASSWORD"),

		// Server Configuration
		Port: os.Getenv("PORT"),

		// Database Configuration
		DBHost:     os.Getenv("DB_HOST"),
		DBUser:     os.Getenv("DB_USER"),
		DBPassword: os.Getenv("DB_PASSWORD"),
		DBName:     os.Getenv("DB_NAME"),
		DBPort:     os.Getenv("DB_PORT"),

		// JWT Configuration
		JWTSecret:          os.Getenv("JWT_SECRET"),
		JWTRefreshSecret:   os.Getenv("JWT_REFRESH_SECRET"),
		JWTMoneroPaySecret: os.Getenv("JWT_MONEROPAY_SECRET"),

		// MoneroPay API Configuration
		MoneroPayBaseURL:     os.Getenv("MONEROPAY_BASE_URL"),
		MoneroPayCallbackURL: os.Getenv("MONEROPAY_CALLBACK_URL"),

		// Monero Wallet RPC Configuration
		MoneroWalletRPCEndpoint: os.Getenv("MONERO_WALLET_RPC_ENDPOINT"),
		MoneroWalletRPCUsername: os.Getenv("MONERO_WALLET_RPC_USERNAME"),
		MoneroWalletRPCPassword: os.Getenv("MONERO_WALLET_RPC_PASSWORD"),
	}

	// Validate required fields
	if config.AdminName == "" ||
		config.AdminPassword == "" ||
		config.Port == "" ||
		config.DBHost == "" ||
		config.DBUser == "" ||
		config.DBPassword == "" ||
		config.DBName == "" ||
		config.DBPort == "" ||
		config.JWTSecret == "" ||
		config.JWTRefreshSecret == "" ||
		config.JWTMoneroPaySecret == "" ||
		config.MoneroPayBaseURL == "" ||
		config.MoneroPayCallbackURL == "" ||
		config.MoneroWalletRPCEndpoint == "" {
		return nil, fmt.Errorf("missing required environment variables")
	}

	return config, nil
}
