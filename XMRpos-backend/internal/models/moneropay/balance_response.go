package moneropay

type BalanceResponse struct {
	Total    int64 `json:"total"`
	Unlocked int64 `json:"unlocked"`
}
