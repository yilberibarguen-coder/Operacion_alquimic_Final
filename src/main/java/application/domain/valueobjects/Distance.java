package application.domain.valueobjects;

import application.domain.exceptions.InvalidDistanceException;

/** Distance from a device to the laboratory. It is a finite number that cannot be negative. */
public record Distance(double value) {

    public Distance {
        if (!Double.isFinite(value)) {
            throw new InvalidDistanceException("The distance must be a finite number.");
        }
        if (value < 0) {
            throw new InvalidDistanceException("The distance cannot be negative.");
        }
    }
}
