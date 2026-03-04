package com.example.rideshare.service;

import com.example.rideshare.model.dto.UserRequestDto;
import com.example.rideshare.model.dto.UserResponseDto;
import java.util.List;

public interface UserService {
    List<UserResponseDto> getAllUsers();

    UserResponseDto getUserById(Long id);

    UserResponseDto createUser(UserRequestDto userDto);

    UserResponseDto updateUser(Long id, UserRequestDto userDto);

    void deleteUser(Long id);

    UserResponseDto getUserWithRides(Long id);

    UserResponseDto getUserByEmail(String email);
}