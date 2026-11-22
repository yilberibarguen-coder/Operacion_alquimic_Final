package application.domain.services;

import application.domain.exceptions.ReadingNotFoundException;
import application.domain.models.DeviceReading;
import application.domain.ports.out.ReadingRepositoryPort;

/** F4 - Replaces completely the reading of a device; the other devices are untouched. */
public class ReplaceReadingService {

    private final ReadingRepositoryPort readingRepositoryPort;

    public ReplaceReadingService(ReadingRepositoryPort readingRepositoryPort) {
        this.readingRepositoryPort = readingRepositoryPort;
    }

    public DeviceReading replace(DeviceReading reading) {
        if (!readingRepositoryPort.replace(reading)) {
            throw new ReadingNotFoundException("The device " + reading.getDevice() + " has no registered reading to replace.");
        }
        return reading;
    }
}
