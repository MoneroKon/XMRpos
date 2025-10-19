package admin

import (
	"encoding/json"
	"net/http"
	"time"
	"context"
	"io"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/utils"
)

type AdminHandler struct {
	service *AdminService
}

func NewAdminHandler(service *AdminService) *AdminHandler {
	return &AdminHandler{service: service}
}

type createInviteRequest struct {
	ValidUntil int64   `json:"valid_until"`
	ForcedName *string `json:"forced_name"`
}

type createInviteResponse struct {
	InviteCode string `json:"invite_code"`
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
