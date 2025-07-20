package moneropay

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"

	"github.com/joho/godotenv"
)

// ExternalAPIClient interacts with an external API
type MoneroPayAPIClient struct {
	BaseURL string
}

// NewExternalAPIClient initializes an external API client with a base URL loaded from environment variables
func NewMoneroPayAPIClient() *MoneroPayAPIClient {
	// Load environment variables from .env file
	if err := godotenv.Load(); err != nil {
		log.Fatal("Error loading .env file")
	}

	// Read the base URL from environment variables
	baseURL := os.Getenv("MONEROPAY_BASE_URL")
	if baseURL == "" {
		log.Fatal("MONEROPAY_BASE_URL environment variable is not set")
	}

	return &MoneroPayAPIClient{BaseURL: baseURL}
}

// GetHealth fetches the health status of services from the MoneroPay API
func (client *MoneroPayAPIClient) GetHealth() (*HealthResponse, error) {
	url := fmt.Sprintf("%s/health", client.BaseURL)

	resp, err := http.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to fetch health status: %s", resp.Status)
	}

	var healthResp HealthResponse
	if err := json.NewDecoder(resp.Body).Decode(&healthResp); err != nil {
		return nil, err
	}

	return &healthResp, nil
}

// GetBalance fetches the balance from the MoneroPay API
func (client *MoneroPayAPIClient) GetBalance() (*BalanceResponse, error) {
	url := fmt.Sprintf("%s/balance", client.BaseURL)

	resp, err := http.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to fetch balance: %s", resp.Status)
	}

	var balanceResp BalanceResponse
	if err := json.NewDecoder(resp.Body).Decode(&balanceResp); err != nil {
		return nil, err
	}

	return &balanceResp, nil
}

// PostReceive creates a new receive address in the MoneroPay API, takes ReceiveRequest as input
func (client *MoneroPayAPIClient) PostReceive(req *ReceiveRequest) (*ReceiveResponse, error) {
	url := fmt.Sprintf("%s/receive", client.BaseURL)

	jsonReq, err := json.Marshal(req)
	if err != nil {
		return nil, err
	}

	resp, err := http.Post(url, "application/json", bytes.NewBuffer(jsonReq))
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to create receive address: %s", resp.Status)
	}

	var receiveResp ReceiveResponse
	if err := json.NewDecoder(resp.Body).Decode(&receiveResp); err != nil {
		return nil, err
	}

	return &receiveResp, nil
}

// GetReceiveAddress fetches incoming transfers for a subaddress. Takes address as input
type GetReceiveAddressParams struct {
	MinHeight *int64
	MaxHeight *int64
}

func (client *MoneroPayAPIClient) GetReceiveAddress(address string, params *GetReceiveAddressParams) (*ReceiveAddressResponse, error) {
	url := fmt.Sprintf("%s/receive/%s", client.BaseURL, address)

	// Add optional query parameters for minHeight and maxHeight if they are provided
	if params.MinHeight != nil {
		url += fmt.Sprintf("?minHeight=%d", *params.MinHeight)
	}
	if params.MaxHeight != nil {
		if params.MinHeight != nil {
			url += fmt.Sprintf("&maxHeight=%d", *params.MaxHeight)
		} else {
			url += fmt.Sprintf("?maxHeight=%d", *params.MaxHeight)
		}
	}

	resp, err := http.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to fetch receive address: %s", resp.Status)
	}

	var receiveAddressResp ReceiveAddressResponse
	if err := json.NewDecoder(resp.Body).Decode(&receiveAddressResp); err != nil {
		return nil, err
	}

	return &receiveAddressResp, nil
}

// PostTransfer creates a new transfer in the MoneroPay API, takes TransferRequest as input
func (client *MoneroPayAPIClient) PostTransfer(req *TransferRequest) (*TransferResponse, error) {
	url := fmt.Sprintf("%s/transfer", client.BaseURL)

	jsonReq, err := json.Marshal(req)
	if err != nil {
		return nil, err
	}

	resp, err := http.Post(url, "application/json", bytes.NewBuffer(jsonReq))
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to create transfer: %s", resp.Status)
	}

	var transferResp TransferResponse
	if err := json.NewDecoder(resp.Body).Decode(&transferResp); err != nil {
		return nil, err
	}

	return &transferResp, nil
}

// GetTransfer fetches a transfer by tx_hash from the MoneroPay API
func (client *MoneroPayAPIClient) GetTransfer(txHash string) (*TransferInformationResponse, error) {
	url := fmt.Sprintf("%s/transfer/%s", client.BaseURL, txHash)

	resp, err := http.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to fetch transfer: %s", resp.Status)
	}

	var transferResp TransferInformationResponse
	if err := json.NewDecoder(resp.Body).Decode(&transferResp); err != nil {
		return nil, err
	}

	return &transferResp, nil
}
