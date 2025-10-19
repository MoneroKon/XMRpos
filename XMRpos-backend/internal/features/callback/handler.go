package callback

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"time"

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
	// Bound request time and size to avoid stuck handlers
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()
	r = r.WithContext(ctx)
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

	var req moneropay.CallbackResponse
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	jwtToken := chi.URLParam(r, "jwt")

	if err := h.service.HandleCallback(ctx, jwtToken, req); err != nil {
		http.Error(w, "callback handling failed", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	resp := "OK"
	_ = json.NewEncoder(w).Encode(resp)
	io.Copy(io.Discard, r.Body)
}
