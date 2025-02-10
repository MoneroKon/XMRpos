package moneropay

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
