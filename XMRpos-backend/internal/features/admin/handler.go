package admin

import (
	"encoding/json"
	"net/http"
	"time"

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

	var req createInviteRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	inviteCode, err := h.service.CreateInvite(time.Unix(req.ValidUntil, 0), req.ForcedName)

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	resp := createInviteResponse{
		InviteCode: inviteCode,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}
