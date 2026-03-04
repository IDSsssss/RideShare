package com.example.rideshare.repository;

import com.example.rideshare.model.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByStartPointAndEndPoint(String startPoint, String endPoint);

    List<Route> findByStartPointContainingIgnoreCase(String startPoint);

    List<Route> findByEndPointContainingIgnoreCase(String endPoint);
}