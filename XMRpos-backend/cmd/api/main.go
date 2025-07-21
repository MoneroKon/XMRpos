package main

import (
	"log"

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

	// Use the Server struct with graceful shutdown
	srv := server.NewServer(cfg, database)
	log.Printf("Starting server on 0.0.0.0:" + cfg.Port)
	log.Fatal(srv.Start())
}
