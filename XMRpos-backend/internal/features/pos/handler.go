package pos

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"
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
	Id      uint   `json:"id"`
	Address string `json:"address"`
}

type listTransactionsResponse struct {
	ConfirmedTransactions []ConfirmedTransactionSummary `json:"confirmed_transactions"`
	PendingTransactions   []PendingTransactionSummary   `json:"pending_transactions"`
}

func (h *PosHandler) CreateTransaction(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 8*time.Second)
	defer cancel()
	r = r.WithContext(ctx)
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB cap

	var req createTransactionRequest
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&req); err != nil {
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

	id, address, err := h.service.CreateTransaction(ctx, *vendorIDPtr, *posIDPtr, req.Amount, req.Description, req.AmountInCurrency, req.Currency, req.RequiredConfirmations)
	if err != nil {
		http.Error(w, "Failed to create transaction", http.StatusInternalServerError)
		return
	}

	resp := createTransactionResponse{
		Id:      id,
		Address: address,
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}

func (h *PosHandler) GetTransaction(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)

	vars := chi.URLParam(r, "id")
	transactionID, err := strconv.ParseUint(vars, 10, 64)
	if err != nil {
		http.Error(w, "Invalid transaction ID", http.StatusBadRequest)
		return
	}
	transactionIDUint := uint(transactionID)

	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	if !ok || role != "pos" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}
	vendorIDPtr, _ := r.Context().Value(models.ClaimsVendorIDKey).(*uint)
	posIDPtr, _ := r.Context().Value(models.ClaimsPosIDKey).(*uint)
	if vendorIDPtr == nil || posIDPtr == nil {
		http.Error(w, "Vendor ID and POS ID are required", http.StatusBadRequest)
		return
	}

	transaction, httpErr := h.service.GetTransaction(ctx, transactionIDUint, *vendorIDPtr, *posIDPtr)
	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(transaction)
}

func (h *PosHandler) ListTransactions(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)

	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	if !ok || role != "pos" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	vendorIDPtr, _ := r.Context().Value(models.ClaimsVendorIDKey).(*uint)
	posIDPtr, _ := r.Context().Value(models.ClaimsPosIDKey).(*uint)
	if vendorIDPtr == nil || posIDPtr == nil {
		http.Error(w, "Vendor ID and POS ID are required", http.StatusBadRequest)
		return
	}

	result, err := h.service.ListTransactionsByPos(ctx, *vendorIDPtr, *posIDPtr)
	if err != nil {
		http.Error(w, "Failed to list transactions", http.StatusInternalServerError)
		return
	}

	resp := listTransactionsResponse{
		ConfirmedTransactions: result.Confirmed,
		PendingTransactions:   result.Pending,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}
