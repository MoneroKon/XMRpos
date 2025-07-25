package vendor

import (
	"encoding/json"
	"net/http"

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
	var req createVendorRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	httpErr := h.service.CreateVendor(req.Name, req.Password, req.InviteCode)

	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}

	resp := "Vendor created successfully"

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

func (h *VendorHandler) DeleteVendor(w http.ResponseWriter, r *http.Request) {
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

	httpErr := h.service.DeleteVendor(*(vendorID.(*uint)))
	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}

	resp := "Vendor deleted successfully"
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

type createPosRequest struct {
	Name     string `json:"name"`
	Password string `json:"password"`
}

func (h *VendorHandler) CreatePos(w http.ResponseWriter, r *http.Request) {
	var req createPosRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
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

	httpErr := h.service.CreatePos(req.Name, req.Password, *(vendorID.(*uint)))

	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}

	resp := "POS created successfully"

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

type getBalanceResponse struct {
	Balance int64 `json:"balance"`
}

func (h *VendorHandler) GetBalance(w http.ResponseWriter, r *http.Request) {
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

	balance, httpErr := h.service.GetBalance(*(vendorID.(*uint)))
	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}

	resp := getBalanceResponse{
		Balance: *balance,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

type transferBalanceRequest struct {
	Address string `json:"address"`
}

func (h *VendorHandler) TransferBalance(w http.ResponseWriter, r *http.Request) {
	var req transferBalanceRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
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

	httpErr := h.service.CreateTransfer(*(vendorID.(*uint)), req.Address)
	if httpErr != nil {
		http.Error(w, httpErr.Message, httpErr.Code)
		return
	}

	resp := "Transfer initiated successfully"
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}
