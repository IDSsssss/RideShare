package com.example.rideshare.service;

import com.example.rideshare.dto.UserDto;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.UserMapper;
import com.example.rideshare.model.User;
import com.example.rideshare.repository.UserRepository;
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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto createUser(UserDto userDTO) {
        User user = userMapper.toEntity(userDTO);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto userDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        existingUser.setName(userDTO.getName());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setPhone(userDTO.getPhone());

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
        User user = userRepository.findByIdWithRides(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
    }
}