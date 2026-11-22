package application.domain.services;

import application.domain.exceptions.InconsistentDistancesException;
import application.domain.valueobjects.Distance;
import application.domain.valueobjects.Position;
import application.domain.valueobjects.SurveillanceDevice;

import java.util.Map;

/**
 * Domain service: finds the laboratory by trilateration from the three devices.
 * Subtracting the circle equations of STAR Labs from the other two gives a 2x2 linear system in (x, y).
 */
public final class LabLocator {

    // Absolute error in map units; accepts distances rounded to two decimal places.
    private static final double ABSOLUTE_TOLERANCE = 0.1;
    private static final double RELATIVE_TOLERANCE = 1e-9;

    /** Needs the distance reported by every device. The result is rounded to two decimals. */
    public Position locate(Map<SurveillanceDevice, Distance> distances) {
        Position p1 = SurveillanceDevice.STAR_LABS.getPosition();
        Position p2 = SurveillanceDevice.CCPD.getPosition();
        Position p3 = SurveillanceDevice.STAR_SATELLITE.getPosition();
        double d1 = distances.get(SurveillanceDevice.STAR_LABS).value();
        double d2 = distances.get(SurveillanceDevice.CCPD).value();
        double d3 = distances.get(SurveillanceDevice.STAR_SATELLITE).value();

        double a = 2 * (p2.x() - p1.x());
        double b = 2 * (p2.y() - p1.y());
        double c = d1 * d1 - d2 * d2 + p2.x() * p2.x() - p1.x() * p1.x() + p2.y() * p2.y() - p1.y() * p1.y();
        double d = 2 * (p3.x() - p1.x());
        double e = 2 * (p3.y() - p1.y());
        double f = d1 * d1 - d3 * d3 + p3.x() * p3.x() - p1.x() * p1.x() + p3.y() * p3.y() - p1.y() * p1.y();

        double determinant = a * e - b * d; // never zero: the three devices are not on one line
        double x = (c * e - b * f) / determinant;
        double y = (a * f - c * d) / determinant;
        Position candidate = new Position(x, y);
        // Check before output rounding so formatting cannot change validity.
        for (SurveillanceDevice device : SurveillanceDevice.values()) {
            Position devicePosition = device.getPosition();
            double reported = distances.get(device).value();
            double calculated = Math.hypot(x - devicePosition.x(), y - devicePosition.y());
            double tolerance = ABSOLUTE_TOLERANCE + RELATIVE_TOLERANCE * Math.max(reported, calculated);
            if (Math.abs(calculated - reported) > tolerance) {
                throw new InconsistentDistancesException(
                        "The distances do not describe a common laboratory position: " + device + ".");
            }
        }
        return candidate.rounded();
    }
}
