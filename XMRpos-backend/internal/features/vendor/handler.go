package vendor

import (
	"encoding/json"
	"net/http"
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
