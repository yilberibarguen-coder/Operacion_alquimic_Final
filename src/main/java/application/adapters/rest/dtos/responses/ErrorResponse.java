package application.adapters.rest.dtos.responses;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String error, String message) {
}
