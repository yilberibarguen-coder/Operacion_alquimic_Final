package application.domain.models;

import application.domain.valueobjects.Position;

/** Result of the processing: where the laboratory is and what the reconstructed message says. */
public record AlchemyReport(Position position, String message) {
}
