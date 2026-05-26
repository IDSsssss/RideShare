package com.example.rideshare.service.impl;

import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ForbiddenException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.ReviewMapper;
import com.example.rideshare.security.CurrentUserAccessor;
import com.example.rideshare.model.dto.ReviewRequestDto;
import com.example.rideshare.model.dto.ReviewResponseDto;
import com.example.rideshare.model.entity.Booking;
import com.example.rideshare.model.entity.Review;
import com.example.rideshare.model.entity.Ride;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.model.enums.RideStatus;
import com.example.rideshare.repository.ReviewRepository;
import com.example.rideshare.repository.RideRepository;
import com.example.rideshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.anyLong;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Unit Tests")
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testDriver;
    private User testReviewer;
    private Ride testRide;
    private Review testReview;
    private ReviewRequestDto testRequest;
    private ReviewResponseDto testResponse;

    @BeforeEach
    void setUp() {
        testDriver = new User();
        testDriver.setId(10L);
        testDriver.setName("Test Driver");
        testDriver.setRating(4.5);

        testReviewer = new User();
        testReviewer.setId(1L);
        testReviewer.setName("Test Reviewer");

        Booking testBooking = new Booking();
        testBooking.setPassenger(testReviewer);

        List<Booking> bookings = new ArrayList<>();
        bookings.add(testBooking);

        testRide = new Ride();
        testRide.setId(100L);
        testRide.setDriver(testDriver);
        testRide.setStatus(RideStatus.COMPLETED);
        testRide.setBookings(bookings);

        testReview = new Review();
        testReview.setId(1000L);
        testReview.setRating(5);
        testReview.setComment("Great ride!");
        testReview.setReviewer(testReviewer);
        testReview.setRide(testRide);
        testReview.setCreatedAt(LocalDateTime.now());

        testRequest = new ReviewRequestDto();
        testRequest.setRideId(100L);
        testRequest.setReviewerId(1L);
        testRequest.setRating(5);
        testRequest.setComment("Great ride!");

        testResponse = new ReviewResponseDto();
        testResponse.setId(1000L);
        testResponse.setRating(5);
        testResponse.setComment("Great ride!");
    }

    private void stubAuthenticatedPassenger() {
        when(currentUserAccessor.currentUserIdOrNull()).thenReturn(1L);
        when(currentUserAccessor.isAdmin()).thenReturn(false);
    }

    @Nested
    @DisplayName("createReview() tests")
    class CreateReviewTests {

        @Test
        @DisplayName("Should create review successfully when user is passenger")
        void createReview_SuccessAsPassenger_ShouldReturnReviewResponse() {
            stubAuthenticatedPassenger();
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(reviewRepository.existsByReviewerIdAndRideId(1L, 100L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testReviewer));
            when(reviewMapper.toEntity(any(ReviewRequestDto.class))).thenReturn(testReview);
            when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
            when(reviewMapper.toResponseDto(any(Review.class))).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingForDriver(10L)).thenReturn(4.8);
            when(userRepository.findById(10L)).thenReturn(Optional.of(testDriver));

            ReviewResponseDto result = reviewService.createReview(testRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1000L);
            verify(reviewRepository, times(1)).save(any(Review.class));
        }

        @Test
        @DisplayName("Should create review successfully when user is driver")
        void createReview_SuccessAsDriver_ShouldReturnReviewResponse() {
            when(currentUserAccessor.currentUserIdOrNull()).thenReturn(10L);
            when(currentUserAccessor.isAdmin()).thenReturn(false);
            ReviewRequestDto request = new ReviewRequestDto();
            request.setRideId(100L);
            request.setReviewerId(10L);
            request.setRating(5);
            request.setComment("Great ride!");

            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(reviewRepository.existsByReviewerIdAndRideId(10L, 100L)).thenReturn(false);
            when(userRepository.findById(10L)).thenReturn(Optional.of(testDriver));
            when(reviewMapper.toEntity(any(ReviewRequestDto.class))).thenReturn(testReview);
            when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
            when(reviewMapper.toResponseDto(any(Review.class))).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingForDriver(10L)).thenReturn(4.8);
            when(userRepository.findById(10L)).thenReturn(Optional.of(testDriver));

            ReviewResponseDto result = reviewService.createReview(request);

            assertThat(result).isNotNull();
            verify(reviewRepository, times(1)).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw exception when ride not found")
        void createReview_RideNotFound_ShouldThrowException() {
            stubAuthenticatedPassenger();
            when(rideRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(testRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ride not found with id: 100");
            verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw exception when ride is not completed")
        void createReview_RideNotCompleted_ShouldThrowException() {
            stubAuthenticatedPassenger();
            testRide.setStatus(RideStatus.SCHEDULED);
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));

            assertThatThrownBy(() -> reviewService.createReview(testRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot review ride that is not completed");
            verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when reviewer not found in database")
        void createReview_ReviewerNotFound_ShouldThrowResourceNotFoundException() {
            stubAuthenticatedPassenger();
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(testRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reviewer not found with id: 1");
        }

        @Test
        @DisplayName("Should throw exception when user already reviewed this ride")
        void createReview_AlreadyReviewed_ShouldThrowException() {
            stubAuthenticatedPassenger();
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(reviewRepository.existsByReviewerIdAndRideId(1L, 100L)).thenReturn(true);

            assertThatThrownBy(() -> reviewService.createReview(testRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User already reviewed this ride");
            verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when user is neither passenger nor driver")
        void createReview_NeitherPassengerNorDriver_ShouldThrowBusinessException() {
            when(currentUserAccessor.currentUserIdOrNull()).thenReturn(99L);
            when(currentUserAccessor.isAdmin()).thenReturn(false);

            ReviewRequestDto request = new ReviewRequestDto();
            request.setRideId(100L);
            request.setReviewerId(99L);
            request.setRating(5);

            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));

            assertThatThrownBy(() -> reviewService.createReview(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User was not a participant in this ride");
        }

        @Test
        @DisplayName("Should reject unauthenticated caller")
        void createReview_NoActor_ShouldThrowForbidden() {
            when(currentUserAccessor.currentUserIdOrNull()).thenReturn(null);

            assertThatThrownBy(() -> reviewService.createReview(testRequest))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Admin may set reviewerId to a participant")
        void createReview_AdminChoosesReviewer_ShouldSucceed() {
            when(currentUserAccessor.currentUserIdOrNull()).thenReturn(999L);
            when(currentUserAccessor.isAdmin()).thenReturn(true);
            testRequest.setReviewerId(1L);
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(reviewRepository.existsByReviewerIdAndRideId(1L, 100L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testReviewer));
            when(reviewMapper.toEntity(any(ReviewRequestDto.class))).thenReturn(testReview);
            when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
            when(reviewMapper.toResponseDto(any(Review.class))).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingForDriver(10L)).thenReturn(4.8);
            when(userRepository.findById(10L)).thenReturn(Optional.of(testDriver));

            ReviewResponseDto result = reviewService.createReview(testRequest);

            assertThat(result).isNotNull();
            verify(reviewRepository, times(1)).save(any(Review.class));
        }
    }

    @Nested
    @DisplayName("getReviewsByRide() tests")
    class GetReviewsByRideTests {

        @Test
        @DisplayName("Should return list of reviews for ride")
        void getReviewsByRide_Success_ShouldReturnReviewsList() {
            List<Review> reviews = Arrays.asList(testReview, testReview);
            List<ReviewResponseDto> expectedResponse = Arrays.asList(testResponse, testResponse);
            when(reviewRepository.findByRideId(100L)).thenReturn(reviews);
            when(reviewMapper.toResponseDtoList(reviews)).thenReturn(expectedResponse);

            List<ReviewResponseDto> result = reviewService.getReviewsByRide(100L);

            assertThat(result).hasSize(2);
            verify(reviewRepository, times(1)).findByRideId(100L);
        }

        @Test
        @DisplayName("Should throw exception when rideId is null")
        void getReviewsByRide_NullRideId_ShouldThrowException() {
            assertThatThrownBy(() -> reviewService.getReviewsByRide(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ride ID cannot be null");
            verify(reviewRepository, never()).findByRideId(any());
        }

        @Test
        @DisplayName("Should return empty list when no reviews for ride")
        void getReviewsByRide_NoReviews_ShouldReturnEmptyList() {
            when(reviewRepository.findByRideId(100L)).thenReturn(List.of());
            when(reviewMapper.toResponseDtoList(List.of())).thenReturn(List.of());

            List<ReviewResponseDto> result = reviewService.getReviewsByRide(100L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getReviewsByUser() tests")
    class GetReviewsByUserTests {

        @Test
        @DisplayName("Should return list of reviews for user")
        void getReviewsByUser_Success_ShouldReturnReviewsList() {
            List<Review> reviews = Arrays.asList(testReview, testReview);
            List<ReviewResponseDto> expectedResponse = Arrays.asList(testResponse, testResponse);
            when(reviewRepository.findByReviewerId(1L)).thenReturn(reviews);
            when(reviewMapper.toResponseDtoList(reviews)).thenReturn(expectedResponse);

            List<ReviewResponseDto> result = reviewService.getReviewsByUser(1L);

            assertThat(result).hasSize(2);
            verify(reviewRepository, times(1)).findByReviewerId(1L);
        }

        @Test
        @DisplayName("Should return empty list when no reviews for user")
        void getReviewsByUser_NoReviews_ShouldReturnEmptyList() {
            when(reviewRepository.findByReviewerId(1L)).thenReturn(List.of());
            when(reviewMapper.toResponseDtoList(List.of())).thenReturn(List.of());

            List<ReviewResponseDto> result = reviewService.getReviewsByUser(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when userId is null")
        void getReviewsByUser_NullUserId_ShouldThrowException() {
            assertThatThrownBy(() -> reviewService.getReviewsByUser(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User ID cannot be null");
            verify(reviewRepository, never()).findByReviewerId(any());
        }
    }

    @Nested
    @DisplayName("getAverageRatingForDriver() tests")
    class GetAverageRatingForDriverTests {

        @Test
        @DisplayName("Should return average rating when reviews exist")
        void getAverageRatingForDriver_WithReviews_ShouldReturnAverage() {
            when(reviewRepository.getAverageRatingForDriver(10L)).thenReturn(4.7);

            Double result = reviewService.getAverageRatingForDriver(10L);

            assertThat(result).isEqualTo(4.7);
        }

        @Test
        @DisplayName("Should return 0.0 when no reviews exist")
        void getAverageRatingForDriver_NoReviews_ShouldReturnZero() {
            when(reviewRepository.getAverageRatingForDriver(10L)).thenReturn(null);

            Double result = reviewService.getAverageRatingForDriver(10L);

            assertThat(result).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("deleteReview() tests")
    class DeleteReviewTests {

        @Test
        @DisplayName("Should delete review successfully")
        void deleteReview_Success_ShouldDeleteReview() {
            when(reviewRepository.existsById(1000L)).thenReturn(true);
            doNothing().when(reviewRepository).deleteById(1000L);

            reviewService.deleteReview(1000L);

            verify(reviewRepository, times(1)).deleteById(1000L);
        }

        @Test
        @DisplayName("Should throw exception when review not found")
        void deleteReview_ReviewNotFound_ShouldThrowException() {
            when(reviewRepository.existsById(1000L)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.deleteReview(1000L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Review not found with id: 1000");
            verify(reviewRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("updateDriverRating() tests (private method verification)")
    class UpdateDriverRatingTests {

        @Test
        @DisplayName("Should update driver rating when creating review")
        void updateDriverRating_ShouldBeCalledWhenCreatingReview() {
            stubAuthenticatedPassenger();
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(reviewRepository.existsByReviewerIdAndRideId(1L, 100L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testReviewer));
            when(reviewMapper.toEntity(any(ReviewRequestDto.class))).thenReturn(testReview);
            when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
            when(reviewMapper.toResponseDto(any(Review.class))).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingForDriver(10L)).thenReturn(4.8);
            when(userRepository.findById(10L)).thenReturn(Optional.of(testDriver));

            reviewService.createReview(testRequest);

            verify(reviewRepository, times(1)).getAverageRatingForDriver(10L);
            verify(userRepository, times(1)).findById(10L);
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Should not update driver rating if driver not found")
        void updateDriverRating_DriverNotFound_ShouldNotUpdate() {
            stubAuthenticatedPassenger();
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(reviewRepository.existsByReviewerIdAndRideId(1L, 100L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testReviewer));
            when(reviewMapper.toEntity(any(ReviewRequestDto.class))).thenReturn(testReview);
            when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
            when(reviewMapper.toResponseDto(any(Review.class))).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingForDriver(10L)).thenReturn(4.8);
            when(userRepository.findById(10L)).thenReturn(Optional.empty());

            reviewService.createReview(testRequest);

            verify(reviewRepository, times(1)).getAverageRatingForDriver(10L);
            verify(userRepository, times(1)).findById(10L);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should set driver rating to 0.0 when average rating is null")
        void updateDriverRating_WhenAvgRatingNull_ShouldSetRatingToZero() {
            stubAuthenticatedPassenger();
            when(rideRepository.findById(100L)).thenReturn(Optional.of(testRide));
            when(reviewRepository.existsByReviewerIdAndRideId(1L, 100L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testReviewer));
            when(reviewMapper.toEntity(any(ReviewRequestDto.class))).thenReturn(testReview);
            when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
            when(reviewMapper.toResponseDto(any(Review.class))).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingForDriver(10L)).thenReturn(null); // ← null
            when(userRepository.findById(10L)).thenReturn(Optional.of(testDriver));
            when(userRepository.save(any(User.class))).thenReturn(testDriver);

            reviewService.createReview(testRequest);

            verify(reviewRepository, times(1)).getAverageRatingForDriver(10L);
            verify(userRepository, times(1)).findById(10L);
            verify(userRepository, times(1)).save(argThat(user ->
                    user.getRating() == 0.0  // проверяем, что рейтинг установлен в 0.0
            ));
        }
    }
}