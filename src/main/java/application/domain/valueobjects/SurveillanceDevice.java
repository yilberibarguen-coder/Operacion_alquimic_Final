package application.domain.valueobjects;

import application.domain.exceptions.UnknownDeviceException;

import java.util.Locale;

/** The three surveillance devices of Central City and their fixed positions. */
public enum SurveillanceDevice {
    STAR_LABS("STAR Labs", new Position(-500, -200)),
    CCPD("CCPD", new Position(100, -100)),
    STAR_SATELLITE("S.T.A.R. Satellite", new Position(500, 100));

    private final String displayName;
    private final Position position;

    SurveillanceDevice(String displayName, Position position) {
        this.displayName = displayName;
        this.position = position;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Position getPosition() {
        return position;
    }

    /**
     * Resolves a device from its identification, ignoring case, dots, spaces and hyphens:
     * "STAR_LABS", "star-labs", "STAR Labs" and "S.T.A.R. Satellite" are all accepted.
     */
    public static SurveillanceDevice from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UnknownDeviceException("The device identification is required.");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(".", "").replaceAll("[\\s-]+", "_");
        for (SurveillanceDevice device : values()) {
            if (device.name().equals(normalized)) {
                return device;
            }
        }
        throw new UnknownDeviceException("Unknown device '" + raw + "'. Valid devices: STAR_LABS, CCPD, STAR_SATELLITE.");
    }
}
