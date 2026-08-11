package com.saadaoui.tunidesk.dashboard.service;

import com.saadaoui.tunidesk.dashboard.dto.DashboardSummaryResponse;

public interface DashboardService {

    DashboardSummaryResponse getSummary(String actorEmail);

}
