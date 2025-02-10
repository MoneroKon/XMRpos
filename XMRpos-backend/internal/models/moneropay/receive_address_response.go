package moneropay

import "time"

type ReceiveAddressResponse struct {
	Amount      Amount      `json:"amount"`
	Complete    bool        `json:"complete"`
	Description string      `json:"description"`
	CreatedAt   time.Time   `json:"created_at"`
	Transaction Transaction `json:"transaction"`
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
	Confirmations   int       `json:"confirmations"`
	DoubleSpendSeen bool      `json:"double_spend_seen"`
	Fee             int64     `json:"fee"`
	Height          int       `json:"height"`
	Timestamp       time.Time `json:"timestamp"`
	TxHash          string    `json:"tx_hash"`
	UnlockTime      int64     `json:"unlock_time"`
	Locked          bool      `json:"locked"`
}
