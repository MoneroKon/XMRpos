package models

import (
	"gorm.io/gorm"
)

type POS struct {
	gorm.Model
	Name               string        `gorm:"unique;not null"`
	PasswordHash       string        `gorm:"not null"`
	PasswordVersion    uint32        `gorm:"not null"`
	VendorID           uint          `gorm:"not null"` // Foreign key field
	Vendor             Vendor        `gorm:"foreignKey:VendorID"`
	DeviceTransactions []Transaction `gorm:"foreignKey:POSID"`
}
