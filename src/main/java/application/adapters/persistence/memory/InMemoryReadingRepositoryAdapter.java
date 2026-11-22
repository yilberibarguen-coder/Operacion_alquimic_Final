package application.adapters.persistence.memory;

import application.domain.models.DeviceReading;
import application.domain.ports.out.ReadingRepositoryPort;
import application.domain.valueobjects.ReadingUpdate;
import application.domain.valueobjects.SurveillanceDevice;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the readings temporarily in memory (no database, no external services).
 * Each operation is atomic thanks to the atomic operations of ConcurrentHashMap.
 */
@Component
public class InMemoryReadingRepositoryAdapter implements ReadingRepositoryPort {

    private final Map<SurveillanceDevice, DeviceReading> readings = new ConcurrentHashMap<>();

    @Override
    public boolean saveIfAbsent(DeviceReading reading) {
        return readings.putIfAbsent(reading.getDevice(), reading) == null;
    }

    @Override
    public boolean replace(DeviceReading reading) {
        return readings.computeIfPresent(reading.getDevice(), (device, stored) -> stored.replacedBy(reading)) != null;
    }

    @Override
    public Optional<DeviceReading> update(SurveillanceDevice device, ReadingUpdate update) {
        return Optional.ofNullable(readings.computeIfPresent(device, (key, stored) -> stored.applyUpdate(update)));
    }

    @Override
    public Optional<DeviceReading> findById(SurveillanceDevice device) {
        return Optional.ofNullable(readings.get(device));
    }

    @Override
    public List<DeviceReading> findAll() {
        return readings.values().stream()
                .sorted(Comparator.comparing(DeviceReading::getDevice))
                .toList();
    }

    @Override
    public boolean deleteById(SurveillanceDevice device) {
        return readings.remove(device) != null;
    }
}
