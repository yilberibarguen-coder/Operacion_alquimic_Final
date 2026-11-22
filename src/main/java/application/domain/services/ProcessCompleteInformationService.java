package application.domain.services;

import application.domain.exceptions.InvalidInformationException;
import application.domain.models.AlchemyReport;
import application.domain.models.DeviceReading;

import java.util.List;

/** F1 - The three devices send everything together: it is processed right away and nothing is stored. */
public class ProcessCompleteInformationService {

    private final AlchemyAnalyzer analyzer;

    public ProcessCompleteInformationService(AlchemyAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public AlchemyReport process(List<DeviceReading> readings) {
        if (readings == null || readings.size() != 3) {
            throw new InvalidInformationException("Exactly three readings, one per device, are required.");
        }
        return analyzer.analyze(readings);
    }
}
