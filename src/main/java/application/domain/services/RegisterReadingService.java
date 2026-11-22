package application.domain.services;

import application.domain.exceptions.ReadingAlreadyExistsException;
import application.domain.models.DeviceReading;
import application.domain.ports.out.ReadingRepositoryPort;

/** F2 - Registers the reading of one device, in any order and at any time. */
public class RegisterReadingService {

    private final ReadingRepositoryPort readingRepositoryPort;

    public RegisterReadingService(ReadingRepositoryPort readingRepositoryPort) {
        this.readingRepositoryPort = readingRepositoryPort;
    }

    public DeviceReading register(DeviceReading reading) {
        if (!readingRepositoryPort.saveIfAbsent(reading)) {
            throw new ReadingAlreadyExistsException("The device " + reading.getDevice()
                    + " already has a registered reading. Use replace (PUT) or partial update (PATCH).");
        }
        return reading;
    }
}
