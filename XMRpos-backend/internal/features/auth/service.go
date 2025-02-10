package auth

import (
	"errors"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"golang.org/x/crypto/bcrypt"
)

type AuthService struct {
	repo   AuthRepository
	config *config.Config
}

func NewAuthService(repo AuthRepository, cfg *config.Config) *AuthService {
	return &AuthService{repo: repo, config: cfg}
}

/*
	 func (s *AuthService) RegisterDevice(name, password string) error {
		hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
		if err != nil {
			return err
		}

		device := &models.Device{
			Name:         name,
			PasswordHash: string(hashedPassword),
		}

		return s.repo.CreateDevice(device)
	}
*/
func (s *AuthService) AuthenticateAdmin(name string, password string) (accessToken string, refreshToken string, err error) {

	if name != s.config.AdminName || password != s.config.AdminPassword {
		return "", "", errors.New("invalid credentials")
	}

	accessToken, refreshToken, err = s.generateAdminToken()
	if err != nil {
		return "", "", errors.New("failed to generate tokens")
	}

	return accessToken, refreshToken, nil
}

func (s *AuthService) AuthenticateVendor(name string, password string) (accessToken string, refreshToken string, err error) {
	vendor, err := s.repo.FindVendorByName(name)
	if err != nil {
		return "", "", errors.New("invalid credentials")
	}

	if err := bcrypt.CompareHashAndPassword([]byte(vendor.PasswordHash), []byte(password)); err != nil {
		return "", "", errors.New("invalid credentials")
	}

	accessToken, refreshToken, err = s.generateVendorToken(vendor.ID, vendor.PasswordVersion)
	if err != nil {
		return "", "", errors.New("failed to generate tokens")
	}

	return accessToken, refreshToken, nil
}

func (s *AuthService) AuthenticatePOS(vendorID uint, name string, password string) (accessToken string, refreshToken string, err error) {
	pos, err := s.repo.FindPOSByVendorIDAndName(vendorID, name)
	if err != nil {
		return "", "", errors.New("invalid credentials")
	}

	if err := bcrypt.CompareHashAndPassword([]byte(pos.PasswordHash), []byte(password)); err != nil {
		return "", "", errors.New("invalid credentials")
	}

	accessToken, refreshToken, err = s.generatePOSToken(vendorID, pos.ID, pos.PasswordVersion)
	if err != nil {
		return "", "", errors.New("failed to generate tokens")
	}

	return accessToken, refreshToken, nil
}

func (s *AuthService) UpdatePassword(deviceID uint, newPassword string) error {
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
	if err != nil {
		return err
	}

	err = s.repo.UpdatePasswordHash(deviceID, string(hashedPassword))
	if err != nil {
		return err
	}

	return nil
}

func (s *AuthService) generateVendorToken(vendorID uint, passwordVersion uint32) (accessToken string, refreshToken string, err error) {
	accessTokenJWT := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"vendor_id":        vendorID,
		"role":             "vendor",
		"password_version": passwordVersion,
		"exp":              time.Now().Add(time.Minute * 5).Unix(),
	})

	refreshTokenJWT := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"vendor_id":        vendorID,
		"role":             "vendor",
		"password_version": passwordVersion,
	})

	accessToken, err = accessTokenJWT.SignedString([]byte(s.config.JWTSecret))
	if err != nil {
		return "", "", err
	}

	refreshToken, err = refreshTokenJWT.SignedString([]byte(s.config.JWTRefreshSecret))
	if err != nil {
		return "", "", err
	}

	return accessToken, refreshToken, nil
}

func (s *AuthService) generatePOSToken(vendorID uint, posID uint, passwordVersion uint32) (accessToken string, refreshToken string, err error) {
	accessTokenJWT := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"vendor_id":        vendorID,
		"role":             "POS",
		"password_version": passwordVersion,
		"pos_id":           posID,
		"exp":              time.Now().Add(time.Minute * 5).Unix(),
	})

	refreshTokenJWT := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"vendor_id":        vendorID,
		"role":             "POS",
		"password_version": passwordVersion,
		"pos_id":           posID,
	})

	accessToken, err = accessTokenJWT.SignedString([]byte(s.config.JWTSecret))
	if err != nil {
		return "", "", err
	}

	refreshToken, err = refreshTokenJWT.SignedString([]byte(s.config.JWTRefreshSecret))
	if err != nil {
		return "", "", err
	}

	return accessToken, refreshToken, nil
}

func (s *AuthService) generateAdminToken() (accessToken string, refreshToken string, err error) {
	accessTokenJWT := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"vendor_id":        0,
		"role":             "admin",
		"password_version": 0,
		"exp":              time.Now().Add(time.Minute * 30).Unix(),
	})

	refreshTokenJWT := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"vendor_id":        0,
		"role":             "admin",
		"password_version": 0,
	})

	accessToken, err = accessTokenJWT.SignedString([]byte(s.config.JWTSecret))
	if err != nil {
		return "", "", err
	}

	refreshToken, err = refreshTokenJWT.SignedString([]byte(s.config.JWTRefreshSecret))
	if err != nil {
		return "", "", err
	}

	return accessToken, refreshToken, nil
}
