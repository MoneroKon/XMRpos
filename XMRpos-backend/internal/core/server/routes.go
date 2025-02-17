package server

import (
	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	localMiddleware "github.com/monerokon/xmrpos/xmrpos-backend/internal/core/server/middleware"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/admin"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/auth"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/features/vendor"

	/* "github.com/monerokon/xmrpos/xmrpos-backend/internal/server/middleware/authmw" */
	"gorm.io/gorm"
)

func NewRouter(cfg *config.Config, db *gorm.DB) *chi.Mux {
	r := chi.NewRouter()

	// Middleware
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)

	// Initialize repositories
	adminRepository := admin.NewAdminRepository(db)
	authRepository := auth.NewAuthRepository(db)
	vendorRepository := vendor.NewVendorRepository(db)

	// Initialize services
	adminService := admin.NewAdminService(adminRepository, cfg)
	authService := auth.NewAuthService(authRepository, cfg)
	vendorService := vendor.NewVendorService(vendorRepository, cfg)

	// Initialize handlers
	adminHandler := admin.NewAdminHandler(adminService)
	authHandler := auth.NewAuthHandler(authService)
	vendorHandler := vendor.NewVendorHandler(vendorService)

	// Public routes
	r.Group(func(r chi.Router) {
		r.Post("/auth/login-admin", authHandler.LoginAdmin)
		r.Post("/auth/login-vendor", authHandler.LoginVendor)
		r.Post("/auth/login-pos", authHandler.LoginPOS)

		r.Post("/vendor/create", vendorHandler.CreateVendor)
	})

	// Protected routes
	r.Group(func(r chi.Router) {
		r.Use(localMiddleware.AuthMiddleware(cfg))

		// Admin routes
		r.Post("/admin/invite", adminHandler.CreateInvite)

		// Vendor routes
		r.Post("/vendor/delete", vendorHandler.DeleteVendor)
		r.Post("/vendor/create-pos", vendorHandler.CreatePOS)

		/* r.Post("/auth/update-password", authHandler.UpdatePassword) */

		/* // Device management
		r.Post("/auth/register", authHandler.RegisterDevice) */

		// Payment routes
		/* r.Get("/balance", paymentHandler.GetBalance)
		r.Post("/receive", paymentHandler.CreatePayment)
		r.Get("/status/{id}", paymentHandler.GetPaymentStatus) */
	})

	return r
}
