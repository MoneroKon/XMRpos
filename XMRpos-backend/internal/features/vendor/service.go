package vendor

import (
	"net/http"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
	"golang.org/x/crypto/bcrypt"
)

type VendorService struct {
	repo   VendorRepository
	config *config.Config
}

func NewVendorService(repo VendorRepository, cfg *config.Config) *VendorService {
	return &VendorService{repo: repo, config: cfg}
}

func (s *VendorService) CreateVendor(name string, password string, inviteCode string) (httpErr *models.HTTPError) {

	if len(name) < 3 || len(name) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "name must be at least 3 characters and no more than 50 characters")
	}

	if len(password) < 8 || len(password) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "password must be at least 8 characters and no more than 50 characters")
	}

	nameTaken, err := s.repo.VendorByNameExists(name)

	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error checking if vendor name exists: "+err.Error())
	}

	if nameTaken {
		return models.NewHTTPError(http.StatusBadRequest, "vendor name already taken")
	}

	invite, err := s.repo.FindInviteByCode(inviteCode)
	if err != nil {
		return models.NewHTTPError(http.StatusBadRequest, "invalid invite code")
	}

	if invite.Used {
		return models.NewHTTPError(http.StatusBadRequest, "invite code already used")
	}

	if invite.ForcedName != nil && *invite.ForcedName != name {
		return models.NewHTTPError(http.StatusBadRequest, "invite code is for a different name")
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error hashing password: "+err.Error())
	}

	vendor := &models.Vendor{
		Name:         name,
		PasswordHash: string(hashedPassword),
	}

	err = s.repo.CreateVendor(vendor)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error creating vendor: "+err.Error())
	}

	err = s.repo.SetInviteToUsed(invite.ID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error setting invite to used: "+err.Error())
	}

	return nil
}

func (s *VendorService) DeleteVendor(vendorID uint) (httpErr *models.HTTPError) {
	vendor, err := s.repo.GetVendorByID(vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error retrieving vendor: "+err.Error())
	}

	if vendor == nil {
		return models.NewHTTPError(http.StatusNotFound, "vendor not found")
	}

	if vendor.Balance != 0 {
		return models.NewHTTPError(http.StatusBadRequest, "vendor balance must be 0 to delete vendor")
	}

	err = s.repo.DeleteAllPosForVendor(vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error deleting POS for vendor: "+err.Error())
	}

	err = s.repo.DeleteAllTransactionsForVendor(vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error deleting transactions for vendor: "+err.Error())
	}

	err = s.repo.DeleteVendor(vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error deleting vendor: "+err.Error())
	}

	return nil
}

func (s *VendorService) CreatePos(name string, password string, vendorID uint) (httpErr *models.HTTPError) {

	if len(name) < 3 || len(name) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "name must be at least 3 characters and no more than 50 characters")
	}

	if len(password) < 8 || len(password) > 50 {
		return models.NewHTTPError(http.StatusBadRequest, "password must be at least 8 characters and no more than 50 characters")
	}

	nameTaken, err := s.repo.PosByNameExistsForVendor(name, vendorID)

	if err != nil {
		return models.NewHTTPError(http.StatusBadRequest, "error checking if POS name exists: "+err.Error())
	}

	if nameTaken {
		return models.NewHTTPError(http.StatusBadRequest, "POS name already taken")
	}

	// check to see if vendor still exists. This is to prevent POS creation on deleted vendor, but probably needs to be done in a better way
	vendor, err := s.repo.GetVendorByID(vendorID)
	if err != nil {
		return models.NewHTTPError(http.StatusBadRequest, "error retrieving vendor: "+err.Error())
	}

	if vendor == nil {
		return models.NewHTTPError(http.StatusBadRequest, "vendor not found")
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error hashing password: "+err.Error())
	}

	pos := &models.Pos{
		Name:         name,
		PasswordHash: string(hashedPassword),
		VendorID:     vendorID,
	}

	err = s.repo.CreatePos(pos)
	if err != nil {
		return models.NewHTTPError(http.StatusInternalServerError, "error creating POS: "+err.Error())
	}

	return nil
}
