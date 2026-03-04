package com.example.rideshare.repository;

import com.example.rideshare.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByNameContainingIgnoreCase(String name);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.ridesAsDriver WHERE u.id = :id")
    Optional<User> findByIdWithRides(@Param("id") Long id);

    boolean existsByEmail(String email);
}