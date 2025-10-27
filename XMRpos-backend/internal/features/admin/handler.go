package admin

import (
	"encoding/json"
	"net/http"
	"time"
	"context"
	"io"

	vendorfeature "github.com/monerokon/xmrpos/xmrpos-backend/internal/features/vendor"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/utils"
)

type AdminHandler struct {
	service *AdminService
	vendorService *vendorfeature.VendorService
}

func NewAdminHandler(service *AdminService, vendorService *vendorfeature.VendorService) *AdminHandler {
	return &AdminHandler{service: service, vendorService: vendorService}
}

type createInviteRequest struct {
	ValidUntil int64   `json:"valid_until"`
	ForcedName *string `json:"forced_name"`
}

type createInviteResponse struct {
	InviteCode string `json:"invite_code"`
}

type walletBalanceResponse struct {
	Total    uint64 `json:"total"`
	Unlocked uint64 `json:"unlocked"`
	Locked   uint64 `json:"locked"`
}

type transferBalanceRequest struct {
	VendorID uint   `json:"vendor_id"`
}

type deleteVendorRequest struct {
	VendorID uint `json:"vendor_id"`
}

type deleteVendorResponse struct {
	Success bool `json:"success"`
	ID      uint `json:"id"`
}

func (h *AdminHandler) CreateInvite(w http.ResponseWriter, r *http.Request) {
	// check jwt if admin

	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)

	if !ok || role != "admin" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
  defer cancel()
  r = r.WithContext(ctx)
  r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB cap

	var req createInviteRequest
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	inviteCode, err := h.service.CreateInvite(ctx, time.Unix(req.ValidUntil, 0), req.ForcedName)

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	resp := createInviteResponse{
		InviteCode: inviteCode,
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}

func (h *AdminHandler) ListVendors(w http.ResponseWriter, r *http.Request) {
	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	if !ok || role != "admin" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)

	vendors, err := h.service.ListVendorsWithBalances(ctx)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	resp := struct {
		Vendors []VendorSummary `json:"vendors"`
	}{Vendors: vendors}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
}

func (h *AdminHandler) GetWalletBalance(w http.ResponseWriter, r *http.Request) {
	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	if !ok || role != "admin" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	if h.vendorService == nil {
		http.Error(w, "Vendor service not configured", http.StatusInternalServerError)
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)

	balance, httpErr := h.vendorService.GetBalance(ctx, 0)
	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}
	if balance == nil {
		http.Error(w, "Failed to retrieve wallet balance", http.StatusInternalServerError)
		return
	}

	resp := walletBalanceResponse{
		Total:    balance.Total,
		Unlocked: balance.Unlocked,
		Locked:   balance.Locked,
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
}

func (h *AdminHandler) TransferBalance(w http.ResponseWriter, r *http.Request) {
	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	if !ok || role != "admin" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	if h.vendorService == nil {
		http.Error(w, "Vendor service not configured", http.StatusInternalServerError)
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), 8*time.Second)
	defer cancel()
	r = r.WithContext(ctx)
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)

	var req transferBalanceRequest
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.VendorID == 0 {
		http.Error(w, "vendor_id is required", http.StatusBadRequest)
		return
	}

	httpErr := h.vendorService.CreateTransfer(ctx, req.VendorID)
	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}

	resp := "Transfer initiated successfully"
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}

func (h *AdminHandler) DeleteVendor(w http.ResponseWriter, r *http.Request) {
	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	if !ok || role != "admin" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)

	var req deleteVendorRequest
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.VendorID == 0 {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		_ = json.NewEncoder(w).Encode(map[string]any{
			"success": false,
			"message": "vendor_id is required",
		})
		io.Copy(io.Discard, r.Body)
		return
	}

	httpErr := h.service.DeleteVendor(ctx, req.VendorID)
	if httpErr != nil {
		message := httpErr.Message
		if httpErr.Code == http.StatusNotFound {
			message = "No vendor with ID"
		}
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(httpErr.Code)
		_ = json.NewEncoder(w).Encode(map[string]any{
			"success": false,
			"message": message,
		})
		io.Copy(io.Discard, r.Body)
		return
	}

	resp := deleteVendorResponse{
		Success: true,
		ID:      req.VendorID,
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}
