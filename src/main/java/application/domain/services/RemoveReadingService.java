package application.domain.services;

import application.domain.exceptions.ReadingNotFoundException;
import application.domain.ports.out.ReadingRepositoryPort;
import application.domain.valueobjects.SurveillanceDevice;

/** F6 - Removes completely the reading of a device; the other devices are untouched. */
public class RemoveReadingService {

    private final ReadingRepositoryPort readingRepositoryPort;

    public RemoveReadingService(ReadingRepositoryPort readingRepositoryPort) {
        this.readingRepositoryPort = readingRepositoryPort;
    }

    public void remove(SurveillanceDevice device) {
        if (!readingRepositoryPort.deleteById(device)) {
            throw new ReadingNotFoundException("The device " + device + " has no registered reading to remove.");
        }
    }
}
