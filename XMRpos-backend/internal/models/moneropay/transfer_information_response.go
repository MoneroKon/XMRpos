package moneropay

import "time"

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
