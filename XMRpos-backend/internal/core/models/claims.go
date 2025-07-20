package models

import (
	"github.com/golang-jwt/jwt/v5"
)

type ClaimsContextKey string

const (
	ClaimsVendorIDKey        ClaimsContextKey = "ClaimsVendorID"
	ClaimsRoleKey            ClaimsContextKey = "ClaimsRole"
	ClaimsPasswordVersionKey ClaimsContextKey = "ClaimsPasswordVersion"
	ClaimsPosIDKey           ClaimsContextKey = "ClaimsPosID"
	ClaimsExpKey             ClaimsContextKey = "ClaimsExp"
)

// Claims represents the custom claims for the JWT token
type Claims struct {
	VendorID        *uint  `json:"vendor_id"`
	Role            string `json:"role"`
	PasswordVersion uint32 `json:"password_version"`
	PosID           *uint  `json:"pos_id"`
	jwt.RegisteredClaims
}
