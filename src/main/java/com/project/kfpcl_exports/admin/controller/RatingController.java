package com.project.kfpcl_exports.admin.controller;

import com.project.kfpcl_exports.admin.model.Rating;
import com.project.kfpcl_exports.admin.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController("adminRatingController")
@RequestMapping("/api/admin/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingRepository ratingRepository;

    @GetMapping
    public ResponseEntity<List<Rating>> getAllRatings() {
        return ResponseEntity.ok(ratingRepository.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRating(@PathVariable Long id) {
        if (ratingRepository.existsById(id)) {
            ratingRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Rating deleted", "success", true));
        }
        return ResponseEntity.notFound().build();
    }
}
