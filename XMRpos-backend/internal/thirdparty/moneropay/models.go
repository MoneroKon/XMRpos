package moneropay

import "time"

type BalanceResponse struct {
	Total    int64 `json:"total"`
	Unlocked int64 `json:"unlocked"`
}

// HealthResponse represents the structure of the health status response from the MoneroPay API
type HealthResponse struct {
	Status   int      `json:"status"`
	Services Services `json:"services"`
}

// Services represents the "services" object inside the health status response
type Services struct {
	Walletrpc  bool `json:"walletrpc"`
	Postgresql bool `json:"postgresql"`
}

type ReceiveAddressResponse struct {
	Amount       Amount        `json:"amount"`
	Complete     bool          `json:"complete"`
	Description  string        `json:"description"`
	CreatedAt    time.Time     `json:"created_at"`
	Transactions []Transaction `json:"transactions"`
}

type CallbackResponse struct {
	Amount      Amount      `json:"amount"`
	Complete    bool        `json:"complete"`
	Description string      `json:"description"`
	CreatedAt   time.Time   `json:"created_at"`
	Transaction Transaction `json:"transaction"`
}

func (cb CallbackResponse) ToReceiveAddressResponse() ReceiveAddressResponse {
	return ReceiveAddressResponse{
		Amount:       cb.Amount,
		Complete:     cb.Complete,
		Description:  cb.Description,
		CreatedAt:    cb.CreatedAt,
		Transactions: []Transaction{cb.Transaction},
	}
}

// Amount represents the amount-related details
type Amount struct {
	Expected int64   `json:"expected"`
	Covered  Covered `json:"covered"`
}

// Covered represents the covered amount details
type Covered struct {
	Total    int64 `json:"total"`
	Unlocked int64 `json:"unlocked"`
}

// Transaction represents each individual transaction in the response
type Transaction struct {
	Amount          int64     `json:"amount"`
	Confirmations   int64     `json:"confirmations"`
	DoubleSpendSeen bool      `json:"double_spend_seen"`
	Fee             int64     `json:"fee"`
	Height          int64     `json:"height"`
	Timestamp       time.Time `json:"timestamp"`
	TxHash          string    `json:"tx_hash"`
	UnlockTime      int64     `json:"unlock_time"`
	Locked          bool      `json:"locked"`
}

type ReceiveRequest struct {
	Amount      int64  `json:"amount"`
	Description string `json:"description"`
	CallbackUrl string `json:"callback_url"`
}

type ReceiveResponse struct {
	Address     string    `json:"address"`
	Amount      int64     `json:"amount"`
	Description string    `json:"description"`
	CreatedAt   time.Time `json:"created_at"`
}

type TransferInformationResponse struct {
	Amount          uint64        `json:"amount"`
	Fee             uint64        `json:"fee"`
	State           string        `json:"state"`
	Transfer        []Destination `json:"transfer"`
	Confirmations   uint64        `json:"confirmations"`
	DoubleSpendSeen bool          `json:"double_spend_seen"`
	Height          uint64        `json:"height"`
	Timestamp       time.Time     `json:"timestamp"`
	UnlockTime      uint64        `json:"unlock_time"`
	TxHash          string        `json:"tx_hash"`
}

type TransferRequest struct {
	Destinations []Destination `json:"destinations"`
}

type Destination struct {
	Amount  int64  `json:"amount"`
	Address string `json:"address"`
}

type TransferResponse struct {
	Amount       int64         `json:"amount"`
	Fee          int64         `json:"fee"`
	TxHash       string        `json:"tx_hash"`
	TxHashList   []string      `json:"tx_hash_list"`
	Destinations []Destination `json:"destinations"`
}
