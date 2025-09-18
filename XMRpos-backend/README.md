# XMRpos-backend

XMRpos-backend is a backend service for managing XMRpos operations. It provides APIs for vendors and POS devices to create and track transactions along with features for multiple POS accounts per vendor.

## Features

- Vendor and POS account management
- Secure authentication using JWT
- Transaction creation and tracking
- MoneroPay integration for payment processing
- Admin invite system
- Health check endpoints
- Transfer completion and withdrawal management

## Getting Started

### Prerequisites

- Go 1.23+
- PostgreSQL database
- MoneroPay API instance
- Monero Wallet RPC

### Configuration

Copy `.env.example` to `.env` and fill in your environment variables:

```sh
cp .env.example .env
```

Edit `.env` to set database credentials, JWT secrets, MoneroPay URLs, and wallet RPC settings.

### Installation

1. Install dependencies:

   ```sh
   go mod tidy
   ```

2. Start the backend server:

   ```sh
   go run ./cmd/api/main.go
   ```

The server will start on the port specified in your `.env` file.

## API Overview

- **Auth**: Login for vendors, POS, and admin.
- **Vendor**: Create vendor, delete vendor, create POS, get balance, initiate transfer.
- **POS**: Create transaction, get transaction details.
- **Admin**: Create invite codes.
- **Misc**: Health check endpoint.

## Project Structure

- `cmd/api/main.go`: Entry point for the server.
- `internal/core/`: Core configuration, models, server setup.
- `internal/features/`: Business logic for vendor, pos, admin, auth, callback, misc.
- `internal/thirdparty/moneropay/`: MoneroPay API client and models.

## Environment Variables

See `.env.example` for all required variables:

- `PORT`: Server port
- `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_PORT`: Database settings
- `JWT_SECRET`, `JWT_REFRESH_SECRET`, `JWT_MONEROPAY_SECRET`: JWT secrets
- `MONEROPAY_BASE_URL`, `MONEROPAY_CALLBACK_URL`: MoneroPay API settings
- `MONERO_WALLET_RPC_ENDPOINT`, `MONERO_WALLET_RPC_USERNAME`, `MONERO_WALLET_RPC_PASSWORD`: Wallet RPC settings (should be same as MoneroPay)
