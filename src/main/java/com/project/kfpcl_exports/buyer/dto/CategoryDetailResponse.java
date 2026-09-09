package com.project.kfpcl_exports.buyer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.kfpcl_exports.buyer.model.Category;

@JsonInclude(JsonInclude.Include.NON_NULL)
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

    // Direct root accessors so if the frontend accesses res.data.name or res.data.id directly, it never fails
    public Long getId() { return category != null ? category.getId() : null; }
    public String getName() { return category != null ? category.getName() : null; }
    public String getDescription() { return category != null ? category.getDescription() : null; }
    public String getImageUrl() { return category != null ? category.getImageUrl() : null; }
    public String getImage() { return getImageUrl(); }
    public Integer getSortOrder() { return category != null ? category.getSortOrder() : 1; }
    public Boolean getIsActive() { return category != null ? category.getIsActive() : true; }
    public Boolean getActive() { return getIsActive(); }
}
