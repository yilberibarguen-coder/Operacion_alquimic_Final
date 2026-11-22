package application.domain.services;

import application.domain.exceptions.InconsistentDistancesException;
import application.domain.exceptions.InconsistentMessageException;
import application.domain.exceptions.InsufficientDataException;
import application.domain.exceptions.ReadingNotFoundException;
import application.domain.models.AccumulatedInformation;
import application.domain.models.DeviceReading;
import application.domain.ports.out.ReadingRepositoryPort;
import application.domain.valueobjects.SurveillanceDevice;

import java.util.List;

/** F3 - Consults what has been registered and tells whether it is enough to process. */
public class ConsultReadingsService {

    private final ReadingRepositoryPort readingRepositoryPort;
    private final AlchemyAnalyzer analyzer;

    public ConsultReadingsService(ReadingRepositoryPort readingRepositoryPort, AlchemyAnalyzer analyzer) {
        this.readingRepositoryPort = readingRepositoryPort;
        this.analyzer = analyzer;
    }

    public AccumulatedInformation consultAll() {
        List<DeviceReading> readings = readingRepositoryPort.findAll();
        List<SurveillanceDevice> missing = analyzer.missingDevices(readings.stream().map(DeviceReading::getDevice).toList());
        try {
            analyzer.analyze(readings);
            return new AccumulatedInformation(readings, missing, true, null);
        } catch (InsufficientDataException | InconsistentMessageException | InconsistentDistancesException e) {
            return new AccumulatedInformation(readings, missing, false, e.getMessage());
        }
    }

    public DeviceReading consultOne(SurveillanceDevice device) {
        return readingRepositoryPort.findById(device)
                .orElseThrow(() -> new ReadingNotFoundException("The device " + device + " has no registered reading."));
    }
}
