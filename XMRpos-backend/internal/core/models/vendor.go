package models

import (
	"gorm.io/gorm"
)

type Vendor struct {
	gorm.Model
	Name            string        `gorm:"unique;not null"`
	PasswordHash    string        `gorm:"not null"`
	PasswordVersion uint32        `gorm:"not null;default:1"`
	Pos             []Pos         `gorm:"foreignKey:VendorID"` // One-to-many relationship with Pos
	Balance         int64         `gorm:"not null;default:0"`
	Transactions    []Transaction `gorm:"foreignKey:VendorID"` // One-to-many relationship with Transactions
	/* WalletAddress   string        `gorm:"not null"` */ // TODO: this will be useful when MoneroPay has implemented mutiple wallets per instance
}
