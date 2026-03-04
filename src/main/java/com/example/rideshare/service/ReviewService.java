package com.example.rideshare.service;

import com.example.rideshare.model.dto.ReviewRequestDto;
import com.example.rideshare.model.dto.ReviewResponseDto;
import java.util.List;

public interface ReviewService {
    ReviewResponseDto createReview(ReviewRequestDto request);

    List<ReviewResponseDto> getReviewsByRide(Long rideId);

    List<ReviewResponseDto> getReviewsByUser(Long userId);

    Double getAverageRatingForDriver(Long driverId);

    void deleteReview(Long reviewId);
}