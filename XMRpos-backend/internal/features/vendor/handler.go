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

	err := h.service.CreateVendor(req.Name, req.Password, req.InviteCode)

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
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

	err := h.service.DeleteVendor(*(vendorID.(*uint)))
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	resp := "Vendor deleted successfully"
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

type createPOSRequest struct {
	Name     string `json:"name"`
	Password string `json:"password"`
}

func (h *VendorHandler) CreatePOS(w http.ResponseWriter, r *http.Request) {
	var req createPOSRequest
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

	err := h.service.CreatePOS(req.Name, req.Password, *(vendorID.(*uint)))

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	resp := "POS created successfully"

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}
