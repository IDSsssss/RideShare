package com.example.rideshare.mapper;

import com.example.rideshare.dto.ReviewDto;
import com.example.rideshare.model.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReviewMapper {

    private final UserMapper userMapper;

    public ReviewDto toDto(Review review) {
        if (review == null) {
            return null;
        }

        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());

        if (review.getReviewer() != null) {
            dto.setReviewer(userMapper.toDto(review.getReviewer()));
        }

        return dto;
    }

    public List<ReviewDto> toDtoList(List<Review> reviews) {
        if (reviews == null) {
            return List.of();
        }

        return reviews.stream().map(this::toDto).collect(Collectors.toList());
    }
}