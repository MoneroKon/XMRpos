package models

import (
	"gorm.io/gorm"
)

type Transfer struct {
	gorm.Model
	VendorID          uint           `gorm:"not null;index"` // Foreign key field
	Vendor            Vendor         `gorm:"foreignKey:VendorID"`
	Amount            int64          `gorm:"not null"`     // Amount to be transferred
	AmountTransferred *int64         `gorm:"default:null"` // Amount that has been transferred (amount - fee)
	Address           string         `gorm:"not null;type:text"`
	TxHash            *string        `gorm:"type:text"`
	Transactions      []*Transaction `gorm:"foreignKey:TransferID"`
	Completed         bool           `gorm:"not null;default:false"` // Indicates if the transfer is completed
}
