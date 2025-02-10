package utils

import (
	"context"

	"github.com/monerokon/xmrpos/xmrpos-backend/internal/core/models"
)

func GetClaimFromContext(ctx context.Context, key models.ClaimsContextKey) (value interface{}, ok bool) {
	value = ctx.Value(key)
	if value == nil {
		return nil, false
	}
	return value, true
}
