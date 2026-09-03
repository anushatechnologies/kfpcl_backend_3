package com.project.kfpcl_exports.buyer.dto;

import com.project.kfpcl_exports.buyer.model.Category;

public class CategoryDetailResponse {
    private Category category;
    private long activeSubcategoryCount;

    public CategoryDetailResponse(Category category, long activeSubcategoryCount) {
        this.category = category;
        this.activeSubcategoryCount = activeSubcategoryCount;
    }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public long getActiveSubcategoryCount() { return activeSubcategoryCount; }
    public void setActiveSubcategoryCount(long activeSubcategoryCount) { this.activeSubcategoryCount = activeSubcategoryCount; }
}
