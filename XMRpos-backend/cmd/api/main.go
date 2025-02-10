package main

import (
	"log"
	"net/http"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	db "github.com/monerokon/xmrpos/xmrpos-backend/internal/core/database"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/server"
)

func main() {
	// Load configuration
	cfg, err := config.LoadConfig()
	if err != nil {
		log.Fatal("Failed to load config:", err)
	}

	// Initialize database
	database, err := db.NewPostgresClient(cfg)
	if err != nil {
		log.Fatal("Failed to connect to database:", err)
	}

	// Setup server with config
	router := server.NewRouter(cfg, database)

	// Start server
	log.Printf("Starting server on :8080")
	log.Fatal(http.ListenAndServe(":8080", router))
}
