package server

import (
	"context"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/rpc"
	localMiddleware "github.com/monerokon/xmrpos/xmrpos-backend/internal/core/server/middleware"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/admin"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/auth"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/callback"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/misc"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/pos"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/vendor"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/thirdparty/moneropay"

	"gorm.io/gorm"
)

// Accept a context tied to server lifecycle to stop background loops on shutdown
func NewRouter(ctx context.Context, cfg *config.Config, db *gorm.DB) *chi.Mux {
	r := chi.NewRouter()

	// Middleware
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)

	moneroPayClient := moneropay.NewMoneroPayAPIClient()

	rpcClient := rpc.NewClient(
		cfg.MoneroWalletRPCEndpoint,
		cfg.MoneroWalletRPCUsername,
		cfg.MoneroWalletRPCPassword,
	)

	// Initialize repositories
	adminRepository := admin.NewAdminRepository(db)
	authRepository := auth.NewAuthRepository(db)
	vendorRepository := vendor.NewVendorRepository(db)
	posRepository := pos.NewPosRepository(db)
	callbackRepository := callback.NewCallbackRepository(db)
	miscRepository := misc.NewMiscRepository(db)

	// Initialize services
	adminService := admin.NewAdminService(adminRepository, cfg)
	authService := auth.NewAuthService(authRepository, cfg)
	vendorService := vendor.NewVendorService(vendorRepository, db, cfg, rpcClient)
	vendorService.StartTransferCompleter(ctx, 30*time.Second) // Check every 30 seconds
	posService := pos.NewPosService(posRepository, cfg, moneroPayClient)
	callbackService := callback.NewCallbackService(callbackRepository, cfg, moneroPayClient)
	callbackService.StartConfirmationChecker(ctx, 30*time.Minute) // Check every 30 minutes
	miscService := misc.NewMiscService(miscRepository, cfg, moneroPayClient)

	// Initialize handlers
	adminHandler := admin.NewAdminHandler(adminService)
	authHandler := auth.NewAuthHandler(authService)
	vendorHandler := vendor.NewVendorHandler(vendorService)
	posHandler := pos.NewPosHandler(posService)
	callbackHandler := callback.NewCallbackHandler(callbackService)
	miscHandler := misc.NewMiscHandler(miscService)

	// Public routes
	r.Group(func(r chi.Router) {
		// Auth routes
		r.Post("/auth/login-admin", authHandler.LoginAdmin)
		r.Post("/auth/login-vendor", authHandler.LoginVendor)
		r.Post("/auth/login-pos", authHandler.LoginPos)
		r.Post("/auth/refresh", authHandler.RefreshToken)

		// Vendor routes
		r.Post("/vendor/create", vendorHandler.CreateVendor)

		// Callback routes
		r.Post("/callback/receive/{jwt}", callbackHandler.ReceiveTransaction)

		// Miscellaneous routes
		r.Get("/misc/health", miscHandler.GetHealth)
	})

	// Protected routes
	r.Group(func(r chi.Router) {
		r.Use(localMiddleware.AuthMiddleware(cfg, authRepository))

		// Auth routes
		r.Post("/auth/update-password", authHandler.UpdatePassword)

		// Admin routes
		r.Post("/admin/invite", adminHandler.CreateInvite)

		// Vendor routes
		r.Post("/vendor/delete", vendorHandler.DeleteVendor)
		r.Post("/vendor/create-pos", vendorHandler.CreatePos)
		r.Get("/vendor/balance", vendorHandler.GetBalance)
		r.Post("/vendor/transfer-balance", vendorHandler.TransferBalance)

		// POS routes
		r.Post("/pos/create-transaction", posHandler.CreateTransaction)
		r.Get("/pos/transaction/{id}", posHandler.GetTransaction)
		r.HandleFunc("/pos/ws/transaction", posHandler.TransactionWS)
	})

	return r
}
