package com.example.rideshare.mapper;

import com.example.rideshare.model.dto.ReviewRequestDto;
import com.example.rideshare.model.dto.ReviewResponseDto;
import com.example.rideshare.model.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReviewMapper {

    private final UserMapper userMapper;

    public ReviewResponseDto toResponseDto(Review review) {
        if (review == null) {
            return null;
        }

        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());

        if (review.getReviewer() != null) {
            dto.setReviewer(userMapper.toResponseDto(review.getReviewer()));
        }

        return dto;
    }

    public Review toEntity(ReviewRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Review review = new Review();
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        return review;
    }

    public List<ReviewResponseDto> toResponseDtoList(List<Review> reviews) {
        if (reviews == null) {
            return List.of();
        }

        return reviews.stream().map(this::toResponseDto).collect(Collectors.toList());
    }
}