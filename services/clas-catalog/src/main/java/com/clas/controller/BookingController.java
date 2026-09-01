package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.dto.BookingRequest;
import com.clas.dto.BookingStatusRequest;
import com.clas.entity.ServiceBooking;
import com.clas.service.BookingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @RequireRole("USER")
    public Result<ServiceBooking> create(@RequestBody BookingRequest request) {
        return Result.ok(bookingService.create(request));
    }

    @GetMapping("/mine")
    @RequireRole("USER")
    public Result<List<ServiceBooking>> mine() {
        return Result.ok(bookingService.mine());
    }

    @PostMapping("/{id}/cancel")
    @RequireRole("USER")
    public Result<ServiceBooking> cancel(@PathVariable Long id) {
        return Result.ok(bookingService.cancelMine(id));
    }

    @GetMapping("/merchant")
    @RequireRole("MERCHANT")
    public Result<List<ServiceBooking>> merchantMine() {
        return Result.ok(bookingService.merchantMine());
    }

    @PostMapping("/{id}/status")
    @RequireRole("MERCHANT")
    public Result<ServiceBooking> updateStatus(@PathVariable Long id, @RequestBody BookingStatusRequest request) {
        return Result.ok(bookingService.updateStatus(id, request.status()));
    }
}
