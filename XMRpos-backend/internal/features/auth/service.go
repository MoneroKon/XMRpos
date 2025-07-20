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

func (s *AuthService) AuthenticatePos(vendorID uint, name string, password string) (accessToken string, refreshToken string, err error) {
	pos, err := s.repo.FindPosByVendorIDAndName(vendorID, name)
	if err != nil {
		return "", "", errors.New("invalid credentials")
	}

	if err := bcrypt.CompareHashAndPassword([]byte(pos.PasswordHash), []byte(password)); err != nil {
		return "", "", errors.New("invalid credentials")
	}

	accessToken, refreshToken, err = s.generatePosToken(vendorID, pos.ID, pos.PasswordVersion)
	if err != nil {
		return "", "", errors.New("failed to generate tokens")
	}

	return accessToken, refreshToken, nil
}

func (s *AuthService) UpdateVendorPassword(vendorID uint, currentPassword string, newPassword string) (accessToken string, newRefreshToken string, err error) {

	// check if the old password is correct
	vendor, err := s.repo.FindVendorByID(vendorID)
	if err != nil {
		return "", "", errors.New("vendor not found")
	}
	if err := bcrypt.CompareHashAndPassword([]byte(vendor.PasswordHash), []byte(currentPassword)); err != nil {
		return "", "", errors.New("invalid current password")
	}
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
	if err != nil {
		return "", "", err
	}

	passwordVersion, err := s.repo.UpdateVendorPasswordHash(vendorID, string(hashedPassword))
	if err != nil {
		return "", "", err
	}

	accessToken, newRefreshToken, err = s.generateVendorToken(vendorID, passwordVersion)
	if err != nil {
		return "", "", err
	}

	return accessToken, newRefreshToken, nil
}

func (s *AuthService) UpdatePosPassword(posID uint, vendorID uint, currentPassword string, newPassword string) (accessToken string, newRefreshToken string, err error) {

	// check if the old password is correct
	pos, err := s.repo.FindPosByID(posID)
	if err != nil {
		return "", "", errors.New("pos not found")
	}
	if err := bcrypt.CompareHashAndPassword([]byte(pos.PasswordHash), []byte(currentPassword)); err != nil {
		return "", "", errors.New("invalid current password")
	}
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
	if err != nil {
		return "", "", err
	}

	passwordVersion, err := s.repo.UpdatePosPasswordHash(posID, string(hashedPassword))
	if err != nil {
		return "", "", err
	}

	accessToken, newRefreshToken, err = s.generatePosToken(vendorID, posID, passwordVersion)
	if err != nil {
		return "", "", err
	}

	return accessToken, newRefreshToken, nil
}

func (s *AuthService) UpdatePosPasswordFromVendor(posID uint, vendorID uint, newPassword string) (accessToken string, newRefreshToken string, err error) {

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
	if err != nil {
		return "", "", err
	}

	passwordVersion, err := s.repo.UpdatePosPasswordHash(posID, string(hashedPassword))
	if err != nil {
		return "", "", err
	}

	accessToken, newRefreshToken, err = s.generatePosToken(vendorID, posID, passwordVersion)
	if err != nil {
		return "", "", err
	}

	return accessToken, newRefreshToken, nil
}

func (s *AuthService) RefreshToken(refreshToken string, vendorID uint, role string, passwordVersion uint32, posID uint) (accessToken string, newRefreshToken string, err error) {
	token, err := jwt.Parse(refreshToken, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, errors.New("invalid signing method")
		}
		return []byte(s.config.JWTRefreshSecret), nil
	})

	if err != nil || !token.Valid {
		return "", "", errors.New("invalid refresh token")
	}

	switch role {
	case "admin":
		return s.generateAdminToken()
	case "vendor":
		// check that the password version matches
		vendor, err := s.repo.FindVendorByID(vendorID)
		if err != nil {
			return "", "", errors.New("invalid credentials")
		}
		if vendor.PasswordVersion != passwordVersion {
			return "", "", errors.New("token is outdated (password changed)")
		}
		return s.generateVendorToken(vendorID, passwordVersion)
	case "pos":
		// check that the password version matches
		pos, err := s.repo.FindPosByID(posID)
		if err != nil {
			return "", "", errors.New("invalid credentials")
		}
		if pos.PasswordVersion != passwordVersion {
			return "", "", errors.New("token is outdated (password changed)")
		}
		return s.generatePosToken(vendorID, posID, passwordVersion)
	default:
		return "", "", errors.New("invalid role in token")
	}
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

func (s *AuthService) generatePosToken(vendorID uint, posID uint, passwordVersion uint32) (accessToken string, refreshToken string, err error) {
	accessTokenJWT := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"vendor_id":        vendorID,
		"role":             "pos",
		"password_version": passwordVersion,
		"pos_id":           posID,
		"exp":              time.Now().Add(time.Minute * 5).Unix(),
	})

	refreshTokenJWT := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"vendor_id":        vendorID,
		"role":             "pos",
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
