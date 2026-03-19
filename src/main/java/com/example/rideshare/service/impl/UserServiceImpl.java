package com.example.rideshare.service.impl;

import com.example.rideshare.model.dto.UserRequestDto;
import com.example.rideshare.model.dto.UserResponseDto;
import com.example.rideshare.model.entity.User;
import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.UserMapper;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private static final String USER_NOT_FOUND = "User not found with id: ";
    private static final String USER_ID_NULL = "User ID cannot be null";
    private static final String USER_EMAIL_EXISTS = "User with email %s already exists";
    private static final String EMAIL_TAKEN = "Email %s is already taken";

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userMapper.toResponseDtoList(userRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        if (id == null) {
            throw new BusinessException(USER_ID_NULL);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));

        return userMapper.toResponseDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto createUser(UserRequestDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new BusinessException(String.format(USER_EMAIL_EXISTS, userDto.getEmail()));
        }

        User user = userMapper.toEntity(userDto);
        User savedUser = userRepository.save(user);

        return userMapper.toResponseDto(savedUser);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long id, UserRequestDto userDto) {
        if (id == null) {
            throw new BusinessException(USER_ID_NULL);
        }

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));

        if (userDto.getEmail() != null && !userDto.getEmail()
                .equals(existingUser.getEmail()) && userRepository.existsByEmail(userDto.getEmail())) {
            throw new BusinessException(String.format(EMAIL_TAKEN, userDto.getEmail()));
        }

        if (userDto.getName() != null) {
            existingUser.setName(userDto.getName());
        }
        if (userDto.getEmail() != null) {
            existingUser.setEmail(userDto.getEmail());
        }
        if (userDto.getPhone() != null) {
            existingUser.setPhone(userDto.getPhone());
        }
        if (userDto.getRating() != null) {
            existingUser.setRating(userDto.getRating());
        }

        User updatedUser = userRepository.save(existingUser);

        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (id == null) {
            throw new BusinessException(USER_ID_NULL);
        }

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(USER_NOT_FOUND + id);
        }

        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserWithRides(Long id) {
        if (id == null) {
            throw new BusinessException(USER_ID_NULL);
        }

        User user = userRepository.findByIdWithRides(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));

        return userMapper.toResponseDto(user);
    }
}