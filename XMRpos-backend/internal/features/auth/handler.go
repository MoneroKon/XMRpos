package auth

import (
	"encoding/json"
	"net/http"
)

type AuthHandler struct {
	service *AuthService
}

func NewAuthHandler(service *AuthService) *AuthHandler {
	return &AuthHandler{service: service}
}

type loginVendorRequest struct {
	Name     string `json:"name"`
	Password string `json:"password"`
}

type loginPOSRequest struct {
	Name     string `json:"name"`
	Password string `json:"password"`
	VendorID uint   `json:"vendor_id"`
}

type loginResponse struct {
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
}

func (h *AuthHandler) LoginAdmin(w http.ResponseWriter, r *http.Request) {
	var req loginVendorRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	accessToken, refreshToken, err := h.service.AuthenticateAdmin(req.Name, req.Password)
	if err != nil {
		http.Error(w, err.Error(), http.StatusUnauthorized)
		return
	}

	resp := loginResponse{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

func (h *AuthHandler) LoginVendor(w http.ResponseWriter, r *http.Request) {
	var req loginVendorRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	accessToken, refreshToken, err := h.service.AuthenticateVendor(req.Name, req.Password)
	if err != nil {
		http.Error(w, err.Error(), http.StatusUnauthorized)
		return
	}

	resp := loginResponse{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

func (h *AuthHandler) LoginPOS(w http.ResponseWriter, r *http.Request) {
	var req loginPOSRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	accessToken, refreshToken, err := h.service.AuthenticatePOS(req.VendorID, req.Name, req.Password)
	if err != nil {
		http.Error(w, err.Error(), http.StatusUnauthorized)
		return
	}

	resp := loginResponse{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

/* func (h *AuthHandler) UpdatePassword(w http.ResponseWriter, r *http.Request) {
	var req loginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	deviceID, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsDeviceIDKey)

	if !ok {
		http.Error(w, "Could not find DeviceID claim", http.StatusInternalServerError)
		return
	}

	deviceIDUint, ok := deviceID.(uint)
	if !ok {
		http.Error(w, "Invalid DeviceID type", http.StatusInternalServerError)
		return
	}

	err := h.service.UpdatePassword(deviceIDUint, req.Password)

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
} */
