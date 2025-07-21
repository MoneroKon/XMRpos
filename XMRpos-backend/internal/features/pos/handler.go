package pos

import (
	"encoding/json"
	"net/http"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/utils"
)

type PosHandler struct {
	service *PosService
}

func NewPosHandler(service *PosService) *PosHandler {
	return &PosHandler{service: service}
}

type createTransactionRequest struct {
	Amount                int64   `json:"amount"`
	Description           *string `json:"description"`
	AmountInCurrency      float64 `json:"amount_in_currency"`
	Currency              string  `json:"currency"`
	RequiredConfirmations int64   `json:"required_confirmations"`
}

type createTransactionResponse struct {
	Address string `json:"address"`
}

func (h *PosHandler) CreateTransaction(w http.ResponseWriter, r *http.Request) {
	var req createTransactionRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.RequiredConfirmations > 10 || req.RequiredConfirmations < 0 {
		http.Error(w, "Required confirmations must be between 0 and 10", http.StatusBadRequest)
		return
	}

	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	if !ok || role != "pos" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	vendorIDPtr, _ := r.Context().Value(models.ClaimsVendorIDKey).(*uint)
	posIDPtr, _ := r.Context().Value(models.ClaimsPosIDKey).(*uint)

	address, err := h.service.CreateTransaction(*vendorIDPtr, *posIDPtr, req.Amount, req.Description, req.AmountInCurrency, req.Currency, req.RequiredConfirmations)
	if err != nil {
		http.Error(w, "Failed to create transaction", http.StatusInternalServerError)
		return
	}

	resp := createTransactionResponse{
		Address: address,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}
