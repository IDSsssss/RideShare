package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.ReviewRequestDto;
import com.example.rideshare.model.dto.ReviewResponseDto;
import com.example.rideshare.model.entity.Review;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.ReviewMapper;
import com.example.rideshare.model.enums.RideStatus;
import com.example.rideshare.repository.ReviewRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewResponseDto createReview(ReviewRequestDto request) {
        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + request.getRideId()));

        if (RideStatus.COMPLETED != ride.getStatus()) {
            throw new BusinessException("Cannot review ride that is not completed");
        }

        boolean wasPassenger = ride.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(request.getReviewerId()));

        if (!wasPassenger && !ride.getDriver().getId().equals(request.getReviewerId())) {
            throw new BusinessException("User was not a participant in this ride");
        }

        if (reviewRepository.existsByReviewerIdAndRideId(request.getReviewerId(), request.getRideId())) {
            throw new BusinessException("User already reviewed this ride");
        }

        User reviewer = userRepository.findById(request.getReviewerId()).orElseThrow(() ->
                new ResourceNotFoundException("Reviewer not found with id: " + request.getReviewerId()));

        Review review = reviewMapper.toEntity(request);
        review.setReviewer(reviewer);
        review.setRide(ride);
        review.setCreatedAt(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);

        updateDriverRating(ride.getDriver().getId());

        return reviewMapper.toResponseDto(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByRide(Long rideId) {
        if (rideId == null) {
            throw new BusinessException("Ride ID cannot be null");
        }

        List<Review> reviews = reviewRepository.findByRideId(rideId);
        return reviewMapper.toResponseDtoList(reviews);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("User ID cannot be null");
        }

        List<Review> reviews = reviewRepository.findByReviewerId(userId);
        return reviewMapper.toResponseDtoList(reviews);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRatingForDriver(Long driverId) {
        Double avgRating = reviewRepository.getAverageRatingForDriver(driverId);
        return avgRating != null ? avgRating : 0.0;
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Review not found with id: " + reviewId);
        }

        reviewRepository.deleteById(reviewId);
    }

    private void updateDriverRating(Long driverId) {
        Double avgRating = reviewRepository.getAverageRatingForDriver(driverId);
        userRepository.findById(driverId).ifPresent(driver -> {
            driver.setRating(avgRating != null ? avgRating : 0.0);
            userRepository.save(driver);
        });
    }
}