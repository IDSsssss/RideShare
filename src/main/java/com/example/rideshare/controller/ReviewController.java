package com.example.rideshare.controller;

import com.example.rideshare.model.dto.ReviewRequestDto;
import com.example.rideshare.model.dto.ReviewResponseDto;
import com.example.rideshare.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController extends BaseController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponseDto> createReview(@Valid @RequestBody ReviewRequestDto request) {
        return created(reviewService.createReview(request));
    }

    @GetMapping("/ride/{rideId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByRide(@PathVariable Long rideId) {
        return ok(reviewService.getReviewsByRide(rideId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByUser(@PathVariable Long userId) {
        return ok(reviewService.getReviewsByUser(userId));
    }

    @GetMapping("/driver/{driverId}/rating")
    public ResponseEntity<Double> getDriverAverageRating(@PathVariable Long driverId) {
        return ok(reviewService.getAverageRatingForDriver(driverId));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return noContent();
    }
}