package moneropay

type TransferRequest struct {
	Destinations []Destination `json:"destinations"`
}

type Destination struct {
	Amount  int64  `json:"amount"`
	Address string `json:"address"`
}
