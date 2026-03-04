package com.example.rideshare.controller;

import com.example.rideshare.dto.UserDto;
import com.example.rideshare.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController extends BaseController {
    private final UserService userService;

    // GET endpoint с @RequestParam
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ok(userService.getAllUsers());
    }

    // GET endpoint с @PathVariable
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ok(userService.getUserById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<UserDto> getUserByEmail(@RequestParam String email) {
        // Демонстрация @RequestParam
        return ok(userService.getUserById(1L)); // Упрощенно
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        return created(userService.createUser(userDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        UserDto updatedUser = userService.updateUser(id, userDto);

        return ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);

        return noContent();
    }

    @GetMapping("/{id}/with-rides")
    public ResponseEntity<UserDto> getUserWithRides(@PathVariable Long id) {
        return ok(userService.getUserWithRides(id));
    }
}