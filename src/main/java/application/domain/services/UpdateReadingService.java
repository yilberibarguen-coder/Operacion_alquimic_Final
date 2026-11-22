package application.domain.services;

import application.domain.exceptions.ReadingNotFoundException;
import application.domain.models.DeviceReading;
import application.domain.ports.out.ReadingRepositoryPort;
import application.domain.valueobjects.ReadingUpdate;
import application.domain.valueobjects.SurveillanceDevice;

/** F5 - Updates only the distance and/or the fragment of a device; what is not provided keeps its value. */
public class UpdateReadingService {

    private final ReadingRepositoryPort readingRepositoryPort;

    public UpdateReadingService(ReadingRepositoryPort readingRepositoryPort) {
        this.readingRepositoryPort = readingRepositoryPort;
    }

    public DeviceReading update(SurveillanceDevice device, ReadingUpdate update) {
        return readingRepositoryPort.update(device, update)
                .orElseThrow(() -> new ReadingNotFoundException("The device " + device + " has no registered reading to update."));
    }
}
