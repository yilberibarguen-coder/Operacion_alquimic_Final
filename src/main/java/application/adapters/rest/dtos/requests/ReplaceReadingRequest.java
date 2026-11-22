package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Full replacement: both the distance and the complete fragment are required. */
public record ReplaceReadingRequest(
        @NotNull Double distance,
        @NotEmpty List<String> message) {
}
