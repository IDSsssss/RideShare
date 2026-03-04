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

    // Константы для сообщений об ошибках
    private static final String USER_NOT_FOUND = "User not found with id: ";
    private static final String USER_ID_NULL = "User ID cannot be null";
    private static final String USER_EMAIL_EXISTS = "User with email %s already exists";
    private static final String EMAIL_TAKEN = "Email %s is already taken";
    private static final String EMAIL_NULL_EMPTY = "Email cannot be null or empty";

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        log.debug("Fetching all users");
        return userMapper.toResponseDtoList(userRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);

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
        log.debug("Creating new user with email: {}", userDto.getEmail());

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new BusinessException(String.format(USER_EMAIL_EXISTS, userDto.getEmail()));
        }

        User user = userMapper.toEntity(userDto);
        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());

        return userMapper.toResponseDto(savedUser);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long id, UserRequestDto userDto) {
        log.debug("Updating user with id: {}", id);

        if (id == null) {
            throw new BusinessException(USER_ID_NULL);
        }

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));

        // Проверка email при обновлении
        if (userDto.getEmail() != null && !userDto.getEmail()
                .equals(existingUser.getEmail()) && userRepository.existsByEmail(userDto.getEmail())) {
            throw new BusinessException(String.format(EMAIL_TAKEN, userDto.getEmail()));
        }

        // Обновление полей
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
        log.info("User updated successfully with id: {}", updatedUser.getId());

        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.debug("Deleting user with id: {}", id);

        if (id == null) {
            throw new BusinessException(USER_ID_NULL);
        }

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(USER_NOT_FOUND + id);
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserWithRides(Long id) {
        log.debug("Fetching user with rides by id: {}", id);

        if (id == null) {
            throw new BusinessException(USER_ID_NULL);
        }

        User user = userRepository.findByIdWithRides(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));

        return userMapper.toResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);

        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(EMAIL_NULL_EMPTY);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("User not found with email: %s", email)));

        return userMapper.toResponseDto(user);
    }
}