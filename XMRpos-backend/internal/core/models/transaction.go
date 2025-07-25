package models

import (
	"time"

	"gorm.io/gorm"
)

type Transaction struct {
	gorm.Model
	VendorID              uint              `gorm:"not null;index"` // Foreign key field
	Vendor                Vendor            `gorm:"foreignKey:VendorID"`
	PosID                 uint              `gorm:"not null;index"` // Foreign key field
	Pos                   Pos               `gorm:"foreignKey:PosID"`
	Amount                int64             `gorm:"not null"`
	RequiredConfirmations int64             `gorm:"not null"`
	Currency              string            `gorm:"not null"`
	AmountInCurrency      float64           `gorm:"not null"`
	Description           *string           `gorm:"type:text"`
	SubAddress            *string           `gorm:"type:text"`
	Accepted              bool              `gorm:"not null;default:false"`
	Confirmed             bool              `gorm:"not null;default:false"`
	Transferred           bool              `gorm:"not null;default:false"`
	SubTransactions       []*SubTransaction `gorm:"foreignKey:TransactionID"`
	TransferID            *uint             `gorm:"index"` // Foreign key, nullable if not all transactions are transferred
	Transfer              *Transfer         `gorm:"foreignKey:TransferID"`
}

type SubTransaction struct {
	gorm.Model
	TransactionID   uint      `gorm:"not null"` // Foreign key field
	Amount          int64     `gorm:"not null"`
	Confirmations   int64     `gorm:"not null"`
	DoubleSpendSeen bool      `gorm:"not null"`
	Fee             int64     `gorm:"not null"`
	Height          int64     `gorm:"not null"`
	Timestamp       time.Time `gorm:"not null"`
	TxHash          string    `gorm:"not null"`
	UnlockTime      int64     `gorm:"not null"`
	Locked          bool      `gorm:"not null"`
}
