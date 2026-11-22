package application.domain.ports.out;

import application.domain.models.DeviceReading;
import application.domain.valueobjects.ReadingUpdate;
import application.domain.valueobjects.SurveillanceDevice;

import java.util.List;
import java.util.Optional;

/**
 * Temporary storage of the readings. Every operation is atomic, so a registration, a replacement,
 * a partial update or a removal never leaves the stored information inconsistent.
 */
public interface ReadingRepositoryPort {

    /** Stores the reading only if the device has none yet. Returns false when it already had one. */
    boolean saveIfAbsent(DeviceReading reading);

    /** Replaces the reading of the device. Returns false when the device had none. */
    boolean replace(DeviceReading reading);

    /** Applies the partial update to the stored reading. Empty when the device had none. */
    Optional<DeviceReading> update(SurveillanceDevice device, ReadingUpdate update);

    Optional<DeviceReading> findById(SurveillanceDevice device);

    /** All the readings, in the fixed order of the devices. */
    List<DeviceReading> findAll();

    /** Removes the reading of the device. Returns false when it had none. */
    boolean deleteById(SurveillanceDevice device);
}
