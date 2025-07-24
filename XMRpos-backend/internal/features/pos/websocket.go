package pos

import (
	"net/http"
	"strconv"
	"sync"

	"github.com/gorilla/websocket"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/utils"
)

type wsClient struct {
	conn          *websocket.Conn
	transactionID uint
}

type wsHub struct {
	clients map[uint][]*wsClient // transactionID -> clients
	mu      sync.Mutex
}

var upgrader = websocket.Upgrader{}

var hub = wsHub{
	clients: make(map[uint][]*wsClient),
}

// WebSocket handler for subscribing to transaction updates
func (h *PosHandler) TransactionWS(w http.ResponseWriter, r *http.Request) {
	transactionIDStr := r.URL.Query().Get("transaction_id")
	if transactionIDStr == "" {
		http.Error(w, "Missing transaction_id query parameter", http.StatusBadRequest)
		return
	}

	transactionID64, err := strconv.ParseUint(transactionIDStr, 10, 64)
	if err != nil {
		http.Error(w, "Invalid transaction_id", http.StatusBadRequest)
		return
	}
	TransactionID := uint(transactionID64)

	// check if the POS is authorized to view this transaction
	role, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	if !ok || role != "pos" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}
	vendorIDPtr, _ := r.Context().Value(models.ClaimsVendorIDKey).(*uint)
	posIDPtr, _ := r.Context().Value(models.ClaimsPosIDKey).(*uint)

	transaction, err := h.service.repo.FindTransactionByID(TransactionID)
	if err != nil {
		http.Error(w, "Transaction not found", http.StatusNotFound)
		return
	}

	if !h.service.IsAuthorizedForTransaction(*vendorIDPtr, *posIDPtr, transaction) {
		http.Error(w, "Unauthorized for this transaction", http.StatusUnauthorized)
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		return
	}
	client := &wsClient{conn: conn, transactionID: TransactionID}

	hub.mu.Lock()
	hub.clients[TransactionID] = append(hub.clients[TransactionID], client)
	hub.mu.Unlock()

	// Keep connection open
	for {
		_, _, err := conn.ReadMessage()
		if err != nil {
			break
		}
	}

	// Remove client on disconnect
	hub.mu.Lock()
	clients := hub.clients[TransactionID]
	for i, c := range clients {
		if c == client {
			hub.clients[TransactionID] = append(clients[:i], clients[i+1:]...)
			break
		}
	}
	hub.mu.Unlock()
	conn.Close()
}

// Call this when a transaction is updated
func NotifyTransactionUpdate(transactionID uint, update interface{}) {
	hub.mu.Lock()
	clients := hub.clients[transactionID]
	hub.mu.Unlock()

	for _, client := range clients {
		client.conn.WriteJSON(update)
	}
}
