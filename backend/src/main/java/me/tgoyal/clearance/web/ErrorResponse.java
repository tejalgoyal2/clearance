package me.tgoyal.clearance.web;

import java.time.Instant;

/** The single JSON shape for every error the API returns. */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
}
