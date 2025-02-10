package middleware

import (
	"context"
	"errors"
	"net/http"
	"reflect"
	"strings"

	"github.com/golang-jwt/jwt/v5"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/config"
	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
)

type contextKey string

func AuthMiddleware(cfg *config.Config) func(next http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			authHeader := r.Header.Get("Authorization")
			if authHeader == "" {
				http.Error(w, "Authorization header missing", http.StatusUnauthorized)
				return
			}

			tokenString := strings.TrimPrefix(authHeader, "Bearer ")
			if tokenString == authHeader {
				http.Error(w, "Invalid authorization header format", http.StatusUnauthorized)
				return
			}

			claims := &models.Claims{}
			token, err := jwt.ParseWithClaims(tokenString, claims, func(token *jwt.Token) (interface{}, error) {
				if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
					return nil, errors.New("invalid signing method")
				}
				return []byte(cfg.JWTSecret), nil
			})

			if err != nil {
				http.Error(w, "Invalid token: "+err.Error(), http.StatusUnauthorized)
				return
			}

			if !token.Valid {
				http.Error(w, "Invalid token", http.StatusUnauthorized)
				return
			}

			ctx := AddClaimsToContext(r.Context(), claims)

			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func AddClaimsToContext(ctx context.Context, claims *models.Claims) context.Context {
	val := reflect.ValueOf(claims).Elem()
	for i := 0; i < val.NumField(); i++ {
		field := val.Field(i)
		fieldName := val.Type().Field(i).Name
		key := models.ClaimsContextKey("Claims" + fieldName)
		ctx = context.WithValue(ctx, key, field.Interface())
	}

	return ctx
}
