package pos

import (
	"context"
	"log"
	"net/http"
	"strconv"
	"sync"
	"time"

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

func uintFromClaim(claim interface{}) (uint, bool) {
	switch v := claim.(type) {
	case uint:
		return v, true
	case *uint:
		if v == nil {
			return 0, false
		}
		return *v, true
	case uint64:
		return uint(v), true
	case int:
		if v < 0 {
			return 0, false
		}
		return uint(v), true
	case int64:
		if v < 0 {
			return 0, false
		}
		return uint(v), true
	case float64:
		if v < 0 {
			return 0, false
		}
		return uint(v), true
	default:
		return 0, false
	}
}

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

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
	// bound auth + repo checks
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()

	// check if the POS is authorized to view this transaction
	roleClaim, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsRoleKey)
	role, _ := roleClaim.(string)
	if !ok || role != "pos" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}
	vendorClaim, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsVendorIDKey)
	if !ok {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}
	vendorID, ok := uintFromClaim(vendorClaim)
	if !ok {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}
	posClaim, ok := utils.GetClaimFromContext(r.Context(), models.ClaimsPosIDKey)
	if !ok {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}
	posID, ok := uintFromClaim(posClaim)
	if !ok {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	transaction, err := h.service.repo.FindTransactionByID(ctx, TransactionID)
	if err != nil {
		http.Error(w, "Transaction not found", http.StatusNotFound)
		return
	}

	if !h.service.IsAuthorizedForTransaction(vendorID, posID, transaction) {
		http.Error(w, "Unauthorized for this transaction", http.StatusUnauthorized)
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("transaction websocket upgrade failed (transactionID=%d, posID=%d, vendorID=%d): %v", TransactionID, posID, vendorID, err)
		http.Error(w, "Failed to upgrade websocket connection: "+err.Error(), http.StatusBadRequest)
		return
	}
	// websocket I/O limits to prevent hangs
	const (
		pongWait   = 60 * time.Second
		pingPeriod = 30 * time.Second
	)
	_ = conn.SetReadDeadline(time.Now().Add(pongWait))
	conn.SetPongHandler(func(string) error { return conn.SetReadDeadline(time.Now().Add(pongWait)) })
	conn.SetReadLimit(1 << 20) // 1MB

	client := &wsClient{conn: conn, transactionID: TransactionID}

	hub.mu.Lock()
	hub.clients[TransactionID] = append(hub.clients[TransactionID], client)
	hub.mu.Unlock()

	defer func() {
		hub.mu.Lock()
		clients := hub.clients[TransactionID]
		for i, c := range clients {
			if c == client {
				hub.clients[TransactionID] = append(clients[:i], clients[i+1:]...)
				break
			}
		}
		if len(hub.clients[TransactionID]) == 0 {
			delete(hub.clients, TransactionID)
		}
		hub.mu.Unlock()
		_ = conn.Close()
	}()

	// ping to detect dead peers
	ticker := time.NewTicker(pingPeriod)
	defer ticker.Stop()

	for {
		select {
		case <-r.Context().Done():
			return
		case <-ticker.C:
			_ = conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
			if err := conn.WriteControl(websocket.PingMessage, nil, time.Now().Add(5*time.Second)); err != nil {
				return
			}
		default:
			_ = conn.SetReadDeadline(time.Now().Add(pongWait))
			if _, _, err := conn.ReadMessage(); err != nil {
				return
			}
		}
	}

}

// Call this when a transaction is updated
func NotifyTransactionUpdate(transactionID uint, update interface{}) {
	hub.mu.Lock()
	clients := hub.clients[transactionID]
	hub.mu.Unlock()

	for _, client := range clients {
		// prevent a slow client from blocking others
		_ = client.conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
		if err := client.conn.WriteJSON(update); err != nil {
			_ = client.conn.Close()
		}
	}
}
