package server

import (
	"context"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"syscall"
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

	return s
}

func (s *Server) Start() error {
	// Root context for router and background workers
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	s.router = NewRouter(ctx, s.config, s.db)

	handler := http.TimeoutHandler(s.router, 15*time.Second, "")
	server := &http.Server{
		Addr:              "0.0.0.0:" + s.config.Port,
		Handler:           handler,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       10 * time.Second,
		WriteTimeout:      30 * time.Second,
		IdleTimeout:       120 * time.Second,
		MaxHeaderBytes:    1 << 20, // 1MB
	}

	// Start server
	errCh := make(chan error, 1)
	go func() {
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			errCh <- err
		}
	}()

	// Wait for signal or fatal server error
	select {
	case <-ctx.Done():
	case err := <-errCh:
		return fmt.Errorf("server error: %w", err)
	}

	// Create shutdown context with timeout
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	// Shutdown server gracefully
	if err := server.Shutdown(shutdownCtx); err != nil {
		return fmt.Errorf("server shutdown failed: %v", err)
	}

	// Close DB pool
	if s.db != nil {
		if sqlDB, err := s.db.DB(); err == nil {
			_ = sqlDB.Close()
		}
	}

	return nil
}
