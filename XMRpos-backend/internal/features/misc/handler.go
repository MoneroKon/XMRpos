package misc

import (
	"encoding/json"
	"net/http"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/thirdparty/moneropay"
)

type MiscHandler struct {
	service *MiscService
}

func NewMiscHandler(service *MiscService) *MiscHandler {
	return &MiscHandler{service: service}
}

type Services struct {
	Postgresql bool                     `json:"postgresql"`
	MoneroPay  moneropay.HealthResponse `json:"MoneroPay"`
}

type HealthResponse struct {
	Status   int      `json:"status"`
	Services Services `json:"services"`
}

func (h *MiscHandler) GetHealth(w http.ResponseWriter, r *http.Request) {

	resp := h.service.GetHealth()

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}
