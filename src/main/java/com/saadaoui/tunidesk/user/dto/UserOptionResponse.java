package com.saadaoui.tunidesk.user.dto;

public record UserOptionResponse(
        java.util.UUID id,
        String firstName,
        String lastName,
        String email
) {
}
