package com.example.rideshare.service.impl;

import com.example.rideshare.exception.BusinessException;
import com.example.rideshare.exception.ConflictException;
import com.example.rideshare.exception.ResourceNotFoundException;
import com.example.rideshare.mapper.UserMapper;
import com.example.rideshare.model.dto.UserRequestDto;
import com.example.rideshare.model.dto.UserResponseDto;
import com.example.rideshare.model.entity.User;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserRequestDto testUserRequestDto;
    private UserResponseDto testUserResponseDto;

    @BeforeEach
    void setUp() {
        // Инициализация тестовых данных
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Иван Петров");
        testUser.setEmail("ivan@example.com");
        testUser.setPhone("+79001234567");
        testUser.setRating(5.0);
        testUser.setCreatedAt(LocalDateTime.now());

        testUserRequestDto = new UserRequestDto();
        testUserRequestDto.setName("Иван Петров");
        testUserRequestDto.setEmail("ivan@example.com");
        testUserRequestDto.setPhone("+79001234567");
        testUserRequestDto.setRating(5.0);

        testUserResponseDto = new UserResponseDto();
        testUserResponseDto.setId(1L);
        testUserResponseDto.setName("Иван Петров");
        testUserResponseDto.setEmail("ivan@example.com");
        testUserResponseDto.setPhone("+79001234567");
        testUserResponseDto.setRating(5.0);
        testUserResponseDto.setCreatedAt(LocalDateTime.now());
    }

    // ==================== GET ALL USERS TESTS ====================

    @Nested
    @DisplayName("getAllUsers() tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return list of all users")
        void getAllUsers_Success_ShouldReturnUsersList() {
            // given
            List<User> users = Arrays.asList(testUser, testUser);
            List<UserResponseDto> expectedResponse = Arrays.asList(testUserResponseDto, testUserResponseDto);
            when(userRepository.findAll()).thenReturn(users);
            when(userMapper.toResponseDtoList(users)).thenReturn(expectedResponse);

            // when
            List<UserResponseDto> result = userService.getAllUsers();

            // then
            assertThat(result).hasSize(2);
            verify(userRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        void getAllUsers_EmptyList_ShouldReturnEmptyList() {
            // given
            when(userRepository.findAll()).thenReturn(List.of());
            when(userMapper.toResponseDtoList(List.of())).thenReturn(List.of());

            // when
            List<UserResponseDto> result = userService.getAllUsers();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== GET USER BY ID TESTS ====================

    @Nested
    @DisplayName("getUserById() tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when id exists")
        void getUserById_Success_ShouldReturnUser() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);

            // when
            UserResponseDto result = userService.getUserById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("ivan@example.com");
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void getUserById_NullId_ShouldThrowException() {
            // when & then
            assertThatThrownBy(() -> userService.getUserById(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User ID cannot be null");
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getUserById_NotFound_ShouldThrowException() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 999");
        }
    }

    // ==================== CREATE USER TESTS ====================

    @Nested
    @DisplayName("createUser() tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully")
        void createUser_Success_ShouldReturnCreatedUser() {
            // given
            when(userRepository.existsByEmail("ivan@example.com")).thenReturn(false);
            when(userMapper.toEntity(testUserRequestDto)).thenReturn(testUser);
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);

            // when
            UserResponseDto result = userService.createUser(testUserRequestDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("ivan@example.com");
            verify(userRepository, times(1)).save(testUser);
        }

        @Test
        @DisplayName("Should throw ConflictException when email already exists")
        void createUser_EmailAlreadyExists_ShouldThrowConflictException() {
            // given
            when(userRepository.existsByEmail("ivan@example.com")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.createUser(testUserRequestDto))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("User with email ivan@example.com already exists");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should create user with default rating when rating is null")
        void createUser_NullRating_ShouldSetDefaultRating() {
            // given
            testUserRequestDto.setRating(null);
            User userWithDefaultRating = new User();
            userWithDefaultRating.setId(1L);
            userWithDefaultRating.setName("Иван Петров");
            userWithDefaultRating.setEmail("ivan@example.com");
            userWithDefaultRating.setPhone("+79001234567");
            userWithDefaultRating.setRating(0.0);

            when(userRepository.existsByEmail("ivan@example.com")).thenReturn(false);
            when(userMapper.toEntity(testUserRequestDto)).thenReturn(userWithDefaultRating);
            when(userRepository.save(any(User.class))).thenReturn(userWithDefaultRating);
            when(userMapper.toResponseDto(any(User.class))).thenReturn(testUserResponseDto);

            // when
            UserResponseDto result = userService.createUser(testUserRequestDto);

            // then
            assertThat(result).isNotNull();
            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    // ==================== UPDATE USER TESTS ====================

    @Nested
    @DisplayName("updateUser() tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully")
        void updateUser_Success_ShouldReturnUpdatedUser() {
            // given
            UserRequestDto updateDto = new UserRequestDto();
            updateDto.setName("Иван Петров Обновленный");
            updateDto.setEmail("ivan.new@example.com");
            updateDto.setPhone("+79009876543");
            updateDto.setRating(4.8);

            User updatedUser = new User();
            updatedUser.setId(1L);
            updatedUser.setName("Иван Петров Обновленный");
            updatedUser.setEmail("ivan.new@example.com");
            updatedUser.setPhone("+79009876543");
            updatedUser.setRating(4.8);

            UserResponseDto updatedResponse = new UserResponseDto();
            updatedResponse.setId(1L);
            updatedResponse.setName("Иван Петров Обновленный");
            updatedResponse.setEmail("ivan.new@example.com");
            updatedResponse.setPhone("+79009876543");
            updatedResponse.setRating(4.8);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("ivan.new@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(updatedUser);
            when(userMapper.toResponseDto(any(User.class))).thenReturn(updatedResponse);

            // when
            UserResponseDto result = userService.updateUser(1L, updateDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Иван Петров Обновленный");
            assertThat(result.getEmail()).isEqualTo("ivan.new@example.com");
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Should update only provided fields")
        void updateUser_OnlyProvidedFields_ShouldUpdateOnlyThoseFields() {
            // given
            UserRequestDto updateDto = new UserRequestDto();
            updateDto.setPhone("+79009998877");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toResponseDto(any(User.class))).thenReturn(testUserResponseDto);

            // when
            UserResponseDto result = userService.updateUser(1L, updateDto);

            // then
            assertThat(result).isNotNull();
            verify(userRepository, times(1)).save(testUser);
            assertThat(testUser.getPhone()).isEqualTo("+79009998877");
            assertThat(testUser.getName()).isEqualTo("Иван Петров");
        }

        @Test
        @DisplayName("Should update only name when other fields null")
        void updateUser_OnlyName_ShouldUpdateOnlyName() {
            // given
            UserRequestDto updateDto = new UserRequestDto();
            updateDto.setName("New Name Only");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toResponseDto(any(User.class))).thenReturn(testUserResponseDto);

            // when
            UserResponseDto result = userService.updateUser(1L, updateDto);

            // then
            assertThat(result).isNotNull();
            verify(userRepository, times(1)).save(testUser);
        }

        @Test
        @DisplayName("Should update only email when other fields null")
        void updateUser_OnlyEmail_ShouldUpdateOnlyEmail() {
            // given
            UserRequestDto updateDto = new UserRequestDto();
            updateDto.setEmail("newemail@example.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toResponseDto(any(User.class))).thenReturn(testUserResponseDto);

            // when
            UserResponseDto result = userService.updateUser(1L, updateDto);

            // then
            assertThat(result).isNotNull();
            assertThat(testUser.getEmail()).isEqualTo("newemail@example.com");
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void updateUser_NullId_ShouldThrowException() {
            // when & then
            assertThatThrownBy(() -> userService.updateUser(null, testUserRequestDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void updateUser_NotFound_ShouldThrowException() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.updateUser(999L, testUserRequestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 999");
        }

        @Test
        @DisplayName("Should throw ConflictException when new email already exists")
        void updateUser_EmailAlreadyExists_ShouldThrowConflictException() {
            // given
            UserRequestDto updateDto = new UserRequestDto();
            updateDto.setEmail("existing@example.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateUser(1L, updateDto))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Email existing@example.com is already taken");
            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ==================== DELETE USER TESTS ====================

    @Nested
    @DisplayName("deleteUser() tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user successfully")
        void deleteUser_Success_ShouldDeleteUser() {
            // given
            when(userRepository.existsById(1L)).thenReturn(true);
            doNothing().when(userRepository).deleteById(1L);

            // when
            userService.deleteUser(1L);

            // then
            verify(userRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void deleteUser_NullId_ShouldThrowException() {
            // when & then
            assertThatThrownBy(() -> userService.deleteUser(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User ID cannot be null");
            verify(userRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void deleteUser_NotFound_ShouldThrowException() {
            // given
            when(userRepository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 999");
            verify(userRepository, never()).deleteById(anyLong());
        }
    }

    // ==================== GET USER WITH RIDES TESTS ====================

    @Nested
    @DisplayName("getUserWithRides() tests")
    class GetUserWithRidesTests {

        @Test
        @DisplayName("Should return user with rides when id exists")
        void getUserWithRides_Success_ShouldReturnUserWithRides() {
            // given
            when(userRepository.findByIdWithRides(1L)).thenReturn(Optional.of(testUser));
            when(userMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);

            // when
            UserResponseDto result = userService.getUserWithRides(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(userRepository, times(1)).findByIdWithRides(1L);
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void getUserWithRides_NullId_ShouldThrowException() {
            // when & then
            assertThatThrownBy(() -> userService.getUserWithRides(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User ID cannot be null");
            verify(userRepository, never()).findByIdWithRides(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getUserWithRides_NotFound_ShouldThrowException() {
            // given
            when(userRepository.findByIdWithRides(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserWithRides(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 999");
        }
    }
}