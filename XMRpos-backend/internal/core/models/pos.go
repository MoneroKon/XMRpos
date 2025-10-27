package models

import (
	"gorm.io/gorm"
)

type Pos struct {
	gorm.Model
	Name               string        `gorm:"not null;uniqueIndex:idx_pos_vendor_id_name,priority:2,where:deleted_at IS NULL"`
	PasswordHash       string        `gorm:"not null"`
	PasswordVersion    uint32        `gorm:"not null;default:1"`
	VendorID           uint          `gorm:"not null;uniqueIndex:idx_pos_vendor_id_name,priority:1,where:deleted_at IS NULL"`
	Vendor             Vendor        `gorm:"foreignKey:VendorID"`
	DeviceTransactions []Transaction `gorm:"foreignKey:PosID"`
}
