package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.ReviewRequestDto;
import com.example.rideshare.model.dto.ReviewResponseDto;
import com.example.rideshare.model.entity.Review;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ForbiddenException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.ReviewMapper;
import com.example.rideshare.model.RideEffectiveStatuses;
import com.example.rideshare.model.enums.RideStatus;
import com.example.rideshare.repository.ReviewRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.security.CurrentUserAccessor;
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
    private final CurrentUserAccessor currentUserAccessor;

    private static final String RIDE_NOT_FOUND = "Ride not found with id: ";
    private static final String RIDE_ID_NULL = "Ride ID cannot be null";
    private static final String USER_ID_NULL = "User ID cannot be null";
    private static final String NOT_PARTICIPANT = "User was not a participant in this ride";
    private static final String REVIEW_NOT_FOUND = "Review not found with id: ";
    private static final String CANNOT_REVIEW = "Cannot review ride that is not completed";
    private static final String ALREADY_REVIEWED = "User already reviewed this ride";
    private static final String REVIEWER_NOT_FOUND = "Reviewer not found with id: ";

    @Override
    @Transactional
    public ReviewResponseDto createReview(ReviewRequestDto request) {
        Long actorId = currentUserAccessor.currentUserIdOrNull();
        if (actorId == null) {
            throw new ForbiddenException("Нужна авторизация для создания отзыва.");
        }

        long reviewerUserId;
        if (currentUserAccessor.isAdmin()) {
            reviewerUserId = request.getReviewerId() != null ? request.getReviewerId() : actorId;
        } else {
            reviewerUserId = actorId;
        }

        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException(RIDE_NOT_FOUND + request.getRideId()));

        LocalDateTime now = LocalDateTime.now();
        if (RideEffectiveStatuses.calculate(ride, now) != RideStatus.COMPLETED) {
            throw new BusinessException(CANNOT_REVIEW);
        }

        boolean wasPassenger = ride.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(reviewerUserId));

        if (!wasPassenger && !ride.getDriver().getId().equals(reviewerUserId)) {
            throw new BusinessException(NOT_PARTICIPANT);
        }

        if (reviewRepository.existsByReviewerIdAndRideId(reviewerUserId, request.getRideId())) {
            throw new BusinessException(ALREADY_REVIEWED);
        }

        User reviewer = userRepository.findById(reviewerUserId).orElseThrow(() ->
                new ResourceNotFoundException(REVIEWER_NOT_FOUND + reviewerUserId));

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
            throw new BusinessException(RIDE_ID_NULL);
        }

        List<Review> reviews = reviewRepository.findByRideId(rideId);
        return reviewMapper.toResponseDtoList(reviews);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(USER_ID_NULL);
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
            throw new ResourceNotFoundException(REVIEW_NOT_FOUND + reviewId);
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