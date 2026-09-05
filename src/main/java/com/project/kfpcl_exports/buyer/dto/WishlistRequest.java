package com.project.kfpcl_exports.buyer.dto;

public class WishlistRequest {
    private Long productId;
    private String buyerId;

    public WishlistRequest() {}

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
}
