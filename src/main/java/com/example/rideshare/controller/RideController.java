package com.example.rideshare.controller;

import com.example.rideshare.model.dto.RideRequestDto;
import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.model.dto.RideSearchRequest;
import com.example.rideshare.model.dto.BulkRideRequestDto;
import com.example.rideshare.service.impl.RideServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rides")
@RequiredArgsConstructor
public class RideController extends BaseController {
    private final RideServiceImpl rideService;

    @GetMapping
    public ResponseEntity<List<RideResponseDto>> getAllRides() {
        return ok(rideService.getAllRides());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideResponseDto> getRideById(@PathVariable Long id) {
        return ok(rideService.getRideById(id));
    }

    @PostMapping
    public ResponseEntity<List<RideResponseDto>> createRide(@Valid @RequestBody BulkRideRequestDto request) {
        return created(rideService.createRidesBulk(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RideResponseDto> updateRide(@PathVariable Long id,
                                                      @Valid @RequestBody RideRequestDto request) {
        return ok(rideService.updateRide(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRide(@PathVariable Long id) {
        rideService.deleteRide(id);
        return noContent();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RideResponseDto> updateRideStatus(@PathVariable Long id,
                                                            @RequestBody Map<String, String> request) {
        return ok(rideService.updateRideStatus(id, request.get("status")));
    }

    @GetMapping("/advanced")
    public ResponseEntity<Page<RideResponseDto>> searchRides(
            @RequestParam(required = false) String startPoint,
            @RequestParam(required = false) String endPoint,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer minSeats,
            @RequestParam(defaultValue = "false") boolean useNative,
            @PageableDefault() Pageable pageable) {

        Pageable convertedPageable = pageable;

        if (useNative && pageable.getSort().isSorted()) {

            Sort newSort = Sort.unsorted();
            for (Sort.Order order : pageable.getSort()) {
                String property = order.getProperty();
                if ("departureTime".equals(property)) {
                    property = "departure_time";
                } else if ("availableSeats".equals(property)) {
                    property = "available_seats";
                }
                newSort = newSort.and(Sort.by(order.getDirection(), property));
            }
            convertedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), newSort);
        }

        RideSearchRequest request = new RideSearchRequest();
        request.setStartPoint(startPoint);
        request.setEndPoint(endPoint);
        request.setFromDate(fromDate);
        request.setToDate(toDate);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        request.setMinSeats(minSeats);
        request.setPageable(convertedPageable);
        request.setUseNative(useNative);

        Page<RideResponseDto> result = rideService.searchRides(request);
        return ok(result);
    }

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ok(rideService.getCacheStats());
    }

    @PostMapping("/cache/invalidate")
    public ResponseEntity<String> invalidateCache() {
        rideService.invalidateCache();
        return ok("Cache invalidated");
    }
}