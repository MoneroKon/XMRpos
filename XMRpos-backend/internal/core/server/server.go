package server

import (
	"context"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"gorm.io/gorm"
)

type Server struct {
	config *config.Config
	db     *gorm.DB
	router *chi.Mux
}

func NewServer(cfg *config.Config, db *gorm.DB) *Server {
	s := &Server{
		config: cfg,
		db:     db,
	}

	s.router = NewRouter(cfg, db)
	return s
}

func (s *Server) Start() error {
	server := &http.Server{
		Addr:         "0.0.0.0:" + s.config.Port,
		Handler:      s.router,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 30 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	// Channel for graceful shutdown
	shutdown := make(chan os.Signal, 1)
	signal.Notify(shutdown, os.Interrupt)

	// Start server in goroutine
	go func() {
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			fmt.Printf("Error starting server: %v\n", err)
			os.Exit(1)
		}
	}()

	// Wait for interrupt signal
	<-shutdown

	// Create shutdown context with timeout
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	// Shutdown server gracefully
	if err := server.Shutdown(ctx); err != nil {
		return fmt.Errorf("server shutdown failed: %v", err)
	}

	return nil
}
