package com.saadaoui.tunidesk.dashboard.controller;

import com.saadaoui.tunidesk.dashboard.dto.DashboardSummaryResponse;
import com.saadaoui.tunidesk.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(
            @AuthenticationPrincipal UserDetails currentUser) {

        return dashboardService.getSummary(currentUser.getUsername());
    }

}
