package com.project.kfpcl_exports.buyer.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BusinessType {
    WHOLESALER,
    TRADER,
    RETAILER;

    @JsonCreator
    public static BusinessType fromString(String value) {
        if (value == null) return null;
        for (BusinessType type : BusinessType.values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid businessType: '" + value + "'. Allowed: WHOLESALER, TRADER, RETAILER");
    }
}
