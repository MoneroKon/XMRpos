package auth

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	/* "github.com/monerokon/xmrpos/xmrpos-backend/internal/core/utils" */)

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

type loginPosRequest struct {
	Name     string `json:"name"`
	Password string `json:"password"`
	VendorID uint   `json:"vendor_id"`
}

type loginResponse struct {
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
}

func (h *AuthHandler) LoginAdmin(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

	var req loginVendorRequest
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	accessToken, refreshToken, err := h.service.AuthenticateAdmin(ctx, req.Name, req.Password)
	if err != nil {
		http.Error(w, err.Error(), http.StatusUnauthorized)
		return
	}

	resp := loginResponse{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}

func (h *AuthHandler) LoginVendor(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)

	var req loginVendorRequest
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	accessToken, refreshToken, err := h.service.AuthenticateVendor(ctx, req.Name, req.Password)
	if err != nil {
		http.Error(w, err.Error(), http.StatusUnauthorized)
		return
	}

	resp := loginResponse{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}

func (h *AuthHandler) LoginPos(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)

	var req loginPosRequest
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	accessToken, refreshToken, err := h.service.AuthenticatePos(ctx, req.VendorID, req.Name, req.Password)
	if err != nil {
		http.Error(w, err.Error(), http.StatusUnauthorized)
		return
	}

	resp := loginResponse{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}

type RefreshTokenRequest struct {
	RefreshToken string `json:"refresh_token"`
}

type RefreshTokenResponse struct {
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
}

func (h *AuthHandler) RefreshToken(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)

	var req RefreshTokenRequest

	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Parse the refresh token to extract claims
	token, err := jwt.Parse(req.RefreshToken, func(token *jwt.Token) (interface{}, error) {
		// You may want to check the signing method here
		return []byte(h.service.config.JWTRefreshSecret), nil
	})

	if err != nil || !token.Valid {
		http.Error(w, "Invalid refresh token", http.StatusUnauthorized)
		return
	}

	claims, ok := token.Claims.(jwt.MapClaims)
	if !ok {
		http.Error(w, "Invalid token claims", http.StatusUnauthorized)
		return
	}

	role, _ := claims["role"].(string)
	vendorID := uint(claims["vendor_id"].(float64))
	passwordVersion := uint32(claims["password_version"].(float64))
	posID := uint(claims["pos_id"].(float64))

	print(role, vendorID, passwordVersion, posID)

	accessToken, refreshToken, err := h.service.RefreshToken(ctx, req.RefreshToken, vendorID, role, passwordVersion, posID)
	if err != nil {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	resp := RefreshTokenResponse{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}

type UpdatePasswordRequest struct {
	CurrentPassword string `json:"current_password,omitempty"` // Optional: always except for vendor updating pos password
	NewPassword     string `json:"new_password"`
	PosID           *uint  `json:"pos_id,omitempty"` // Optional: only for vendor updating POS password
}

func (h *AuthHandler) UpdatePassword(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)

	var req UpdatePasswordRequest
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	role, _ := r.Context().Value(models.ClaimsRoleKey).(string)
	vendorIDPtr, _ := r.Context().Value(models.ClaimsVendorIDKey).(*uint)
	posIDPtr, _ := r.Context().Value(models.ClaimsPosIDKey).(*uint)

	switch role {
	case "vendor":
		if req.PosID != nil {
			// Vendor updating POS password
			if vendorIDPtr == nil {
				http.Error(w, "Invalid vendor_id claim", http.StatusUnauthorized)
				return
			}
			accessToken, refreshToken, err := h.service.UpdatePosPasswordFromVendor(ctx, *vendorIDPtr, *req.PosID, req.NewPassword)
			if err != nil {
				http.Error(w, err.Error(), http.StatusUnauthorized)
				return
			}
			resp := loginResponse{
				AccessToken:  accessToken,
				RefreshToken: refreshToken,
			}
			w.Header().Set("Content-Type", "application/json")
			_ = json.NewEncoder(w).Encode(resp)
			return
		} else {
			// Vendor updating own password
			if vendorIDPtr == nil {
				http.Error(w, "Invalid vendor_id claim", http.StatusUnauthorized)
				return
			}
			accessToken, refreshToken, err := h.service.UpdateVendorPassword(ctx, *vendorIDPtr, req.CurrentPassword, req.NewPassword)
			if err != nil {
				http.Error(w, err.Error(), http.StatusUnauthorized)
				return
			}
			resp := loginResponse{
				AccessToken:  accessToken,
				RefreshToken: refreshToken,
			}
			w.Header().Set("Content-Type", "application/json")
			_ = json.NewEncoder(w).Encode(resp)
			return
		}
	case "pos":
		// POS can only update its own password
		if posIDPtr == nil {
			http.Error(w, "Invalid pos_id claim", http.StatusUnauthorized)
			return
		}
		accessToken, refreshToken, err := h.service.UpdatePosPassword(ctx, *posIDPtr, *vendorIDPtr, req.CurrentPassword, req.NewPassword)
		if err != nil {
			http.Error(w, err.Error(), http.StatusUnauthorized)
			return
		}
		resp := loginResponse{
			AccessToken:  accessToken,
			RefreshToken: refreshToken,
		}
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(resp)
		return
	}

	http.Error(w, "Unauthorized", http.StatusUnauthorized)
	io.Copy(io.Discard, r.Body)
}
