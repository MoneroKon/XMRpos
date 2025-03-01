package moneropay

type ReceiveRequest struct {
	Amount      int64  `json:"amount"`
	Description string `json:"description"`
	CallbackUrl string `json:"callback_url"`
}
