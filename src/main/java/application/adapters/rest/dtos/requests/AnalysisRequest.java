package application.adapters.rest.dtos.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The complete information of the three devices received together. */
public record AnalysisRequest(
        @NotEmpty List<@NotNull @Valid RegisterReadingRequest> devices) {
}
