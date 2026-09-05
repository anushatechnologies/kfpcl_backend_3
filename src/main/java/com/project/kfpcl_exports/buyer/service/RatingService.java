package com.project.kfpcl_exports.buyer.service;

import com.project.kfpcl_exports.buyer.dto.RatingRequest;
import com.project.kfpcl_exports.buyer.model.Product;
import com.project.kfpcl_exports.buyer.model.ProductRating;
import com.project.kfpcl_exports.buyer.repository.ProductRatingRepository;
import com.project.kfpcl_exports.buyer.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RatingService {

    @Autowired
    private ProductRatingRepository ratingRepository;

    @Autowired
    private ProductRepository productRepository;

    public ProductRating addRating(RatingRequest request) {
        Optional<Product> productOpt = productRepository.findById(request.getProductId());
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found with id: " + request.getProductId());
        }
        String buyerId = request.getBuyerId() != null ? request.getBuyerId() : "1";

        ProductRating rating = new ProductRating(
                buyerId,
                productOpt.get(),
                request.getRating() != null ? request.getRating() : 5,
                request.getReview()
        );
        return ratingRepository.save(rating);
    }
}
