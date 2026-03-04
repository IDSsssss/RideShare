package com.example.rideshare.service;

import com.example.rideshare.dto.UserDto;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.UserMapper;
import com.example.rideshare.model.User;
import com.example.rideshare.repository.UserRepository;
import com.example.rideshare.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userMapper.toDtoList(userRepository.findAll());
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        ValidationUtils.validateNotNull(id, "User ID");

        User user = userRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("User not found with id: " + id));

        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto createUser(UserDto userDto) {
        ValidationUtils.validateNotNull(userDto.getEmail(), "Email");
        ValidationUtils.validateNotNull(userDto.getName(), "Name");
        ValidationUtils.validateNotNull(userDto.getPhone(), "Phone");

        if (userDto.getRating() != null) {
            ValidationUtils.validateMin(userDto.getRating(), 0.0, "Rating");
            ValidationUtils.validateMax(userDto.getRating(), 5.0, "Rating");
        }

        User user = userMapper.toEntity(userDto);
        User savedUser = userRepository.save(user);

        return userMapper.toDto(savedUser);
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        ValidationUtils.validateNotNull(id, "User ID");

        User existingUser = userRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("User not found with id: " + id));

        existingUser.setName(userDto.getName());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setPhone(userDto.getPhone());

        User updatedUser = userRepository.save(existingUser);

        return userMapper.toDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public UserDto getUserWithRides(Long id) {
        User user = userRepository.findByIdWithRides(id).orElseThrow(() ->
                new ResourceNotFoundException("User not found with id: " + id));

        return userMapper.toDto(user);
    }
}