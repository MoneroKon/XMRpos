package moneropay

import "time"

type ReceiveResponse struct {
	Address     string    `json:"address"`
	Amount      int64     `json:"amount"`
	Description string    `json:"description"`
	CreatedAt   time.Time `json:"created_at"`
}
