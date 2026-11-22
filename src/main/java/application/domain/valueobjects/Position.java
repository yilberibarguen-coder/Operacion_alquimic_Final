package application.domain.valueobjects;

import application.domain.exceptions.InvalidInformationException;

/** Point (x, y) on the Central City map. */
public record Position(double x, double y) {

    public Position {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new InvalidInformationException("A position needs finite coordinates.");
        }
    }

    /** The same position rounded to two decimals (the precision used to report the laboratory location). */
    public Position rounded() {
        return new Position(round(x), round(y));
    }

    private static double round(double value) {
        double rounded = Math.round(value * 100.0) / 100.0;
        return rounded == 0.0 ? 0.0 : rounded; // avoids reporting -0.0
    }
}
