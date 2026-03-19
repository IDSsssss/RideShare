package com.example.rideshare.controller;

import com.example.rideshare.model.dto.RideResponseDto;
import com.example.rideshare.model.dto.RideSearchRequest;
import com.example.rideshare.service.impl.RideSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/rides/search")
@RequiredArgsConstructor
public class RideSearchController extends BaseController {

    private final RideSearchService rideSearchService;

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
            @PageableDefault(size = 10, sort = "departure_time", direction = Sort.Direction.ASC) Pageable pageable) {

        RideSearchRequest request = new RideSearchRequest();
        request.setStartPoint(startPoint);
        request.setEndPoint(endPoint);
        request.setFromDate(fromDate);
        request.setToDate(toDate);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        request.setMinSeats(minSeats);
        request.setPageable(pageable);
        request.setUseNative(useNative);

        Page<RideResponseDto> result = rideSearchService.searchRides(request);

        return ok(result);
    }

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ok(rideSearchService.getCacheStats());
    }

    @PostMapping("/cache/invalidate")
    public ResponseEntity<String> invalidateCache() {
        rideSearchService.invalidateCache();
        return ok("Cache invalidated");
    }
}