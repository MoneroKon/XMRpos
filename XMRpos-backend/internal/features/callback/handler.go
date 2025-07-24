package callback

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/thirdparty/moneropay"
)

type CallbackHandler struct {
	service *CallbackService
}

func NewCallbackHandler(service *CallbackService) *CallbackHandler {
	return &CallbackHandler{service: service}
}

func (h *CallbackHandler) ReceiveTransaction(w http.ResponseWriter, r *http.Request) {

	var req moneropay.CallbackResponse
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	jwtToken := chi.URLParam(r, "jwt")

	h.service.HandleCallback(jwtToken, req)

	w.WriteHeader(http.StatusOK)
	resp := "OK"
	json.NewEncoder(w).Encode(resp)
}
