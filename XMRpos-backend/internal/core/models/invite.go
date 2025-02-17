package models

import (
	"time"

	"gorm.io/gorm"
)

type Invite struct {
	gorm.Model
	Used       bool      `gorm:"not null default:false"`
	InviteCode string    `gorm:"size:21;unique;uniqueIndex;not null"`
	ForcedName *string   `gorm:"size:255"`
	ValidUntil time.Time `gorm:"not null"`
}
