package server

import (
	"context"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	localMiddleware "github.com/monerokon/xmrpos/xmrpos-backend/internal/core/server/middleware"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/admin"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/auth"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/callback"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/misc"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/pos"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/vendor"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/thirdparty/moneropay"

	/* "github.com/monerokon/xmrpos/xmrpos-backend/internal/server/middleware/authmw" */
	"gorm.io/gorm"
)

func NewRouter(cfg *config.Config, db *gorm.DB) *chi.Mux {
	r := chi.NewRouter()

	// Middleware
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)

	moneroPayClient := moneropay.NewMoneroPayAPIClient()

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
	vendorService := vendor.NewVendorService(vendorRepository, cfg)
	posService := pos.NewPosService(posRepository, cfg, moneroPayClient)
	callbackService := callback.NewCallbackService(callbackRepository, cfg, moneroPayClient)
	callbackService.StartConfirmationChecker(context.Background(), 30*time.Second) // Check every 30 seconds
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

		// POS routes
		r.Post("/pos/create-transaction", posHandler.CreateTransaction)
		r.Get("/pos/transaction/{id}", posHandler.GetTransaction)
		r.HandleFunc("/pos/ws/transaction", posHandler.TransactionWS)

		/* // Device management
		r.Post("/auth/register", authHandler.RegisterDevice) */

		// Payment routes
		/* r.Get("/balance", paymentHandler.GetBalance)
		r.Post("/receive", paymentHandler.CreatePayment)
		r.Get("/status/{id}", paymentHandler.GetPaymentStatus) */
	})

	return r
}
