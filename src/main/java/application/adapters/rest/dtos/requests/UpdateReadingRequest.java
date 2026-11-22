package application.adapters.rest.dtos.requests;

import java.util.List;

/** Partial update: any field that is not sent (or is null) keeps its previous value. */
public record UpdateReadingRequest(
        Double distance,
        List<String> message) {
}
