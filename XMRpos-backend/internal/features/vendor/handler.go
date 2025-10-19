package vendor

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"time"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/utils"
)

type VendorHandler struct {
	service *VendorService
}

func NewVendorHandler(service *VendorService) *VendorHandler {
	return &VendorHandler{service: service}
}

type createVendorRequest struct {
	Name       string `json:"name"`
	Password   string `json:"password"`
	InviteCode string `json:"invite_code"`
}

func (h *VendorHandler) CreateVendor(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)

	var req createVendorRequest
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	httpErr := h.service.CreateVendor(ctx, req.Name, req.Password, req.InviteCode)

	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}

	resp := "Vendor created successfully"

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}

func (h *VendorHandler) DeleteVendor(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)

	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	if !ok || role != "vendor" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	vendorID, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsVendorIDKey)
	if !ok {
		http.Error(w, "Unauthorized: vendorID not found", http.StatusUnauthorized)
		return
	}

	httpErr := h.service.DeleteVendor(ctx, *(vendorID.(*uint)))
	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}

	resp := "Vendor deleted successfully"
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}

type createPosRequest struct {
	Name     string `json:"name"`
	Password string `json:"password"`
}

func (h *VendorHandler) CreatePos(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)

	var req createPosRequest
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	if !ok || role != "vendor" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	vendorID, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsVendorIDKey)
	if !ok {
		http.Error(w, "Unauthorized: vendorID not found", http.StatusUnauthorized)
		return
	}

	httpErr := h.service.CreatePos(ctx, req.Name, req.Password, *(vendorID.(*uint)))

	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}

	resp := "POS created successfully"

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}

type getBalanceResponse struct {
	Balance int64 `json:"balance"`
}

func (h *VendorHandler) GetBalance(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)

	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	if !ok || role != "vendor" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	vendorID, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsVendorIDKey)
	if !ok {
		http.Error(w, "Unauthorized: vendorID not found", http.StatusUnauthorized)
		return
	}

	balance, httpErr := h.service.GetBalance(ctx, *(vendorID.(*uint)))
	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}

	resp := getBalanceResponse{
		Balance: *balance,
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
}

type transferBalanceRequest struct {
	Address string `json:"address"`
}

func (h *VendorHandler) TransferBalance(w http.ResponseWriter, r *http.Request) {
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

	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	if !ok || role != "vendor" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	vendorID, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsVendorIDKey)
	if !ok {
		http.Error(w, "Unauthorized: vendorID not found", http.StatusUnauthorized)
		return
	}

	httpErr := h.service.CreateTransfer(ctx, *(vendorID.(*uint)), req.Address)
	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}

	resp := "Transfer initiated successfully"
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}
