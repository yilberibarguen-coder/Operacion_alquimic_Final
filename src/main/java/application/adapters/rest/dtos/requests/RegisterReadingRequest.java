package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** One device reading: its identification, the distance to the laboratory and its message fragment. */
public record RegisterReadingRequest(
        @NotBlank String deviceId,
        @NotNull Double distance,
        @NotEmpty List<String> message) {
}
