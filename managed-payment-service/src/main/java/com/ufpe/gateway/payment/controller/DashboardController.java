package com.ufpe.gateway.payment.controller;

import com.ufpe.gateway.payment.service.DashboardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardStateService dashboardStateService;

    @GetMapping("/status")
    public DashboardStateService.DashboardSnapshot getStatus() {
        return dashboardStateService.getSnapshot();
    }

    @PostMapping("/mapek-status")
    public ResponseEntity<Void> receiveMapeKStatus(@RequestBody DashboardStateService.MapeKStatusRecord status) {
        dashboardStateService.updateMapeKStatus(status);
        return ResponseEntity.accepted().build();
    }

}