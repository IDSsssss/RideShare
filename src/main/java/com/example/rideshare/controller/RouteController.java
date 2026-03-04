package com.example.rideshare.controller;

import com.example.rideshare.model.dto.RouteRequestDto;
import com.example.rideshare.model.dto.RouteResponseDto;
import com.example.rideshare.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RouteController extends BaseController {

    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<List<RouteResponseDto>> getAllRoutes() {
        return ok(routeService.getAllRoutes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteResponseDto> getRouteById(@PathVariable Long id) {
        return ok(routeService.getRouteById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<RouteResponseDto>> searchRoutes(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return ok(routeService.findRoutesByStartAndEnd(start, end));
    }

    @PostMapping
    public ResponseEntity<RouteResponseDto> createRoute(@Valid @RequestBody RouteRequestDto routeDto) {
        return created(routeService.createRoute(routeDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RouteResponseDto> updateRoute(
            @PathVariable Long id,
            @Valid @RequestBody RouteRequestDto routeDto) {
        return ok(routeService.updateRoute(id, routeDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);

        return noContent();
    }
}