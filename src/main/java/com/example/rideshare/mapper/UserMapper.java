package com.example.rideshare.mapper;

import com.example.rideshare.model.dto.UserRequestDto;
import com.example.rideshare.model.dto.UserResponseDto;
import com.example.rideshare.model.entity.User;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class UserMapper {

    public UserResponseDto toResponseDto(User user) {
        if (user == null) {
            return null;
        }

        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRating(user.getRating());
        dto.setCreatedAt(user.getCreatedAt());

        return dto;
    }

    public User toEntity(UserRequestDto dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRating(dto.getRating() != null ? dto.getRating() : 0.0);

        return user;
    }

    public List<UserResponseDto> toResponseDtoList(List<User> users) {
        if (users == null) {
            return List.of();
        }

        return users.stream().map(this::toResponseDto).toList();
    }
}