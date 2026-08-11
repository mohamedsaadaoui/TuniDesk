package com.saadaoui.tunidesk.sla.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SlaPolicyRequest {

    @NotBlank(message = "SLA name is required")
    @Size(max = 100)
    private String name;

    @NotNull(message = "Resolution hours are required")
    @Min(value = 1, message = "Resolution hours must be at least 1")
    private Integer hours;

}
