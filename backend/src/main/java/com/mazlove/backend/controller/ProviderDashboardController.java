package com.mazlove.backend.controller;

import com.mazlove.backend.dto.ProviderStatsDTO;
import com.mazlove.backend.dto.UserDTO;
import com.mazlove.backend.service.ProviderDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/providers/me")
public class ProviderDashboardController {
    private final ProviderDashboardService dashboard;

    public ProviderDashboardController(ProviderDashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/sessions/upcoming")
    public List<UserDTO.SessionDTO> upcoming() { return dashboard.upcoming(); }

    @GetMapping("/sessions/history")
    public List<UserDTO.SessionDTO> history() { return dashboard.history(); }

    @GetMapping("/stats")
    public ProviderStatsDTO stats() { return dashboard.stats(); }
}
