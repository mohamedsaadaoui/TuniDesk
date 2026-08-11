package com.saadaoui.tunidesk.sla.controller;

import com.saadaoui.tunidesk.sla.dto.SlaPolicyRequest;
import com.saadaoui.tunidesk.sla.dto.SlaPolicyResponse;
import com.saadaoui.tunidesk.sla.service.SlaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sla")
@RequiredArgsConstructor
public class SlaPolicyController {

    private final SlaService slaService;

    @GetMapping("/policies")
    public List<SlaPolicyResponse> getAllPolicies() {

        return slaService.getAllPolicies();
    }

    @PutMapping("/policies/{id}")
    public SlaPolicyResponse updatePolicy(
            @PathVariable UUID id,
            @Valid @RequestBody SlaPolicyRequest request) {

        return slaService.updatePolicy(id, request);
    }

}
