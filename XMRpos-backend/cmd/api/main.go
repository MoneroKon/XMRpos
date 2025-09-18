package main

import (
	"log"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	db "github.com/monerokon/xmrpos/xmrpos-backend/internal/core/database"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/server"
)

func main() {
	cfg, err := config.LoadConfig()
	if err != nil {
		log.Fatal("Failed to load config:", err)
	}

	database, err := db.NewPostgresClient(cfg)
	if err != nil {
		log.Fatal("Failed to connect to database:", err)
	}

	sqlDB, err := database.DB()
	if err != nil {
		log.Fatal("Failed to get underlying sql.DB:", err)
	}
	defer sqlDB.Close()

	srv := server.NewServer(cfg, database)
	log.Printf("Starting server on 0.0.0.0:" + cfg.Port)
	log.Fatal(srv.Start())
}
