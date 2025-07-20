package models

import (
	"time"

	"gorm.io/gorm"
)

type Transaction struct {
	gorm.Model
	VendorID              uint      `gorm:"not null"` // Foreign key field
	Vendor                Vendor    `gorm:"foreignKey:VendorID"`
	PosID                 uint      `gorm:"not null"` // Foreign key field
	Pos                   Pos       `gorm:"foreignKey:PosID"`
	TxHash                string    `gorm:"not null"`
	Amount                int64     `gorm:"not null"`
	SeenInMempool         bool      `gorm:"not null"`
	Confirmations         int       `gorm:"not null"`
	RequiredConfirmations int       `gorm:"not null"`
	Currency              string    `gorm:"not null"`
	AmountInCurrency      float64   `gorm:"not null"`
	Timestamp             time.Time `gorm:"not null"`
	SubAddress            string    `gorm:"not null"`
	Status                string    `gorm:"not null"`
}
