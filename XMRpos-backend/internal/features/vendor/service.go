package vendor

import (
	"errors"

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

func (s *VendorService) CreateVendor(name string, password string, inviteCode string) (err error) {

	if len(name) < 3 || len(name) > 50 {
		return errors.New("name must be at least 3 characters and no more than 50 characters")
	}

	if len(password) < 8 || len(password) > 50 {
		return errors.New("password must be at least 8 characters and no more than 50 characters")
	}

	nameTaken, err := s.repo.VendorByNameExists(name)

	if err != nil {
		return errors.New("error checking if vendor name exists: " + err.Error())
	}

	if nameTaken {
		return errors.New("vendor name already taken")
	}

	invite, err := s.repo.FindInviteByCode(inviteCode)
	if err != nil {
		return errors.New("invalid invite code")
	}

	if invite.Used {
		return errors.New("invite code already used")
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return err
	}

	vendor := &models.Vendor{
		Name:         name,
		PasswordHash: string(hashedPassword),
	}

	err = s.repo.CreateVendor(vendor)
	if err != nil {
		return err
	}

	err = s.repo.SetInviteToUsed(invite.ID)
	if err != nil {
		return err
	}

	return nil
}

func (s *VendorService) DeleteVendor(vendorID uint) error {
	vendor, err := s.repo.GetVendorByID(vendorID)
	if err != nil {
		return err
	}

	if vendor == nil {
		return errors.New("vendor not found")
	}

	if vendor.Balance != 0 {
		return errors.New("vendor balance must be 0 to delete vendor")
	}

	err = s.repo.DeleteAllPOSForVendor(vendorID)
	if err != nil {
		return err
	}

	err = s.repo.DeleteAllTransactionsForVendor(vendorID)
	if err != nil {
		return err
	}

	return s.repo.DeleteVendor(vendorID)
}
