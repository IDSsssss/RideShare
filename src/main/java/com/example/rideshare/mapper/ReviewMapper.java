package com.example.rideshare.mapper;

import com.example.rideshare.dto.ReviewDto;
import com.example.rideshare.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class, RideMapper.class})
public interface ReviewMapper {
    ReviewMapper INSTANCE = Mappers.getMapper(ReviewMapper.class);

    @Mapping(target = "ride", ignore = true)
    ReviewDto toDto(Review review);

    Review toEntity(ReviewDto reviewDTO);

    List<ReviewDto> toDtoList(List<Review> reviews);
}