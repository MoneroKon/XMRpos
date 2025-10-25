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
- MoneroPay API instance (git apply `moneropay.patch`)
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


### MoneroPay + XMRpos-backend: Docker Setup

### Prerequisites

- Docker and Docker Compose
- jq and psql optional for tests

1. Setup MoneroPay
```
git clone https://gitlab.com/moneropay/moneropay.git
cd moneropay
cp .env.example .env
cp docker-compose.override.yaml.example docker-compose.override.yaml
# edit .env and docker-compose.override.yaml
docker compose up -d
```

2. Setup XMRpos-backend
```
git clone https://github.com/MoneroKon/XMRpos
cd XMRpos/XMRpos-backend
cp .env.docker.example .env
# edit .env (use: openssl rand -hex 32 for secrets) and docker-compose.yaml
# if Postgres already uses host 5432, map to a free port: 55432:5432
docker compose up -d --build
```

3. Useful commands
```
# show containers
docker compose ps

# follow logs
docker compose logs -f backend

# restart API
docker compose restart backend

# rebuild API only
docker compose up -d --build backend
```

## How to use it

In the future a web interface should be created for easier usage. For now, you can use tools like Postman or curl to interact with the API.

1. **Login as admin**: Use the `/auth/login-admin` endpoint with admin credentials to obtain a JWT token.
2. **Create an invite**: Use the `/admin/invite` endpoint to create a new invite code.
3. **Register a vendor**: Use the `/auth/register` endpoint with the invite code to create a new vendor account.
4. **Login vendor**: Use the `/auth/login-vendor` endpoint to obtain a JWT token.
5. **Create POS**: Use the `/vendor/create-pos` endpoint to create a new POS account under the vendor.

Now the POS account can be used with the XMRpos app.

To transfer the balance from the vendor account to the Monero wallet, use the `/vendor/transfer-balance` endpoint. It will not be instant and will group transfers to be able to payout more often. This should happen automatically around every 20 minutes.

### Example: Login as admin

**POST** `/auth/login-admin`

```json
{
  "name": "admin",
  "password": "admin"
}
```

### Example: Create an invite

**POST** `/admin/invite`

```json
{
  "valid_until": "2025-12-31T23:59:59Z",
  "forced_name": null
}
```

### Example: Register a vendor

**POST** `/vendor/create`

```json
{
  "name": "vendor1",
  "password": "yourStrongPassword",
  "invite_code": "ac8eajc3j"
}
```

### Example: Login vendor

**POST** `/auth/login-vendor`

```json
{
  "name": "vendor1",
  "password": "yourStrongPassword"
}
```

### Example: Create POS

**POST** `/vendor/create-pos`

```json
{
  "name": "pos1",
  "password": "yourStrongPassword"
}
```

### Example: Vendor initiate transfer

**POST** `/vendor/transfer-balance`

```json
{
  "address": "your_monero_address"
}
```

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
