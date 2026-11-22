package application.domain.models;

import application.domain.exceptions.InvalidInformationException;
import application.domain.valueobjects.Distance;
import application.domain.valueobjects.MessageFragment;
import application.domain.valueobjects.ReadingUpdate;
import application.domain.valueobjects.SurveillanceDevice;

/**
 * What one surveillance device reported: its distance to the laboratory and its message fragment.
 * The identity is the device. It is immutable: replacing or updating produces a new reading.
 */
public final class DeviceReading {

    private final SurveillanceDevice device;
    private final Distance distance;
    private final MessageFragment fragment;

    public DeviceReading(SurveillanceDevice device, Distance distance, MessageFragment fragment) {
        if (device == null || distance == null || fragment == null) {
            throw new InvalidInformationException("A reading needs the device, the distance and the message fragment.");
        }
        this.device = device;
        this.distance = distance;
        this.fragment = fragment;
    }

    /** F4 - Full replacement: everything is substituted by the new reading, which must be of the same device. */
    public DeviceReading replacedBy(DeviceReading newReading) {
        if (newReading == null || newReading.device != device) {
            throw new InvalidInformationException("The replacement must belong to the same device.");
        }
        return newReading;
    }

    /** F5 - Partial update: only the provided fields change; the others keep their previous value. */
    public DeviceReading applyUpdate(ReadingUpdate update) {
        if (update == null) {
            throw new InvalidInformationException("The update is required.");
        }
        return new DeviceReading(device, update.distance().orElse(distance), update.fragment().orElse(fragment));
    }

    public SurveillanceDevice getDevice() { return device; }
    public Distance getDistance() { return distance; }
    public MessageFragment getFragment() { return fragment; }
}
