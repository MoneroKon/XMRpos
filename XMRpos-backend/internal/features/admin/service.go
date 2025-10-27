package admin

import (
	"context"
	"net/http"
	"time"

	gonanoid "github.com/matoous/go-nanoid/v2"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	vendorfeature "github.com/monerokon/xmrpos/xmrpos-backend/internal/features/vendor"
)

type AdminService struct {
	repo          AdminRepository
	config        *config.Config
	vendorService *vendorfeature.VendorService
}

type VendorSummary struct {
	ID               uint   `json:"id"`
	Name             string `json:"name"`
	MoneroSubaddress string `json:"monero_subaddress"`
	Balance          int64  `json:"balance"`
}

func NewAdminService(repo AdminRepository, cfg *config.Config, vendorService *vendorfeature.VendorService) *AdminService {
	return &AdminService{repo: repo, config: cfg, vendorService: vendorService}
}

func (s *AdminService) CreateInvite(ctx context.Context, validUntil time.Time, forcedName *string) (inviteCode string, err error) {
	if ctx == nil {
		ctx = context.Background()
	}

	inviteCode, err = gonanoid.New()

	if err != nil {
		return "", err
	}

	invite := &models.Invite{
		Used:       false,
		InviteCode: inviteCode,
		ForcedName: forcedName,
		ValidUntil: validUntil,
	}

	_, err = s.repo.CreateInvite(ctx, invite)
	if err != nil {
		return "", err
	}

	return inviteCode, nil
}

func (s *AdminService) ListVendorsWithBalances(ctx context.Context) ([]VendorSummary, error) {
	if ctx == nil {
		ctx = context.Background()
	}

	return s.repo.ListVendorsWithBalances(ctx)
}

func (s *AdminService) DeleteVendor(ctx context.Context, vendorID uint) (httpErr *models.HTTPError) {
	if ctx == nil {
		ctx = context.Background()
	}

	if s.vendorService == nil {
		return models.NewHTTPError(http.StatusInternalServerError, "vendor service not configured")
	}

	if vendorID == 0 {
		return models.NewHTTPError(http.StatusBadRequest, "vendor_id is required")
	}

	return s.vendorService.DeleteVendor(ctx, vendorID)
}
