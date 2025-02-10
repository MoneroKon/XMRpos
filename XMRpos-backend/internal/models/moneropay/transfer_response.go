package moneropay

type TransferResponse struct {
	Amount       int64         `json:"amount"`
	Fee          int64         `json:"fee"`
	TxHash       string        `json:"tx_hash"`
	TxHashList   []string      `json:"tx_hash_list"`
	Destinations []Destination `json:"destinations"`
}
