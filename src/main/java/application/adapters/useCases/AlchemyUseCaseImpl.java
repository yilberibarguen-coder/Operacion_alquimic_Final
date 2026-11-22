package application.adapters.useCases;

import application.domain.models.AccumulatedInformation;
import application.domain.models.AlchemyReport;
import application.domain.models.DeviceReading;
import application.domain.ports.in.AlchemyPort;
import application.domain.services.ConsultReadingsService;
import application.domain.services.ProcessAccumulatedService;
import application.domain.services.ProcessCompleteInformationService;
import application.domain.services.RegisterReadingService;
import application.domain.services.RemoveReadingService;
import application.domain.services.ReplaceReadingService;
import application.domain.services.UpdateReadingService;
import application.domain.valueobjects.ReadingUpdate;
import application.domain.valueobjects.SurveillanceDevice;
import org.springframework.stereotype.Service;

import java.util.List;

/** Implements the input port by delegating each operation to its domain service. */
@Service
public class AlchemyUseCaseImpl implements AlchemyPort {

    private final ProcessCompleteInformationService processCompleteInformationService;
    private final RegisterReadingService registerReadingService;
    private final ConsultReadingsService consultReadingsService;
    private final ProcessAccumulatedService processAccumulatedService;
    private final ReplaceReadingService replaceReadingService;
    private final UpdateReadingService updateReadingService;
    private final RemoveReadingService removeReadingService;

    public AlchemyUseCaseImpl(ProcessCompleteInformationService processCompleteInformationService,
                              RegisterReadingService registerReadingService,
                              ConsultReadingsService consultReadingsService,
                              ProcessAccumulatedService processAccumulatedService,
                              ReplaceReadingService replaceReadingService,
                              UpdateReadingService updateReadingService,
                              RemoveReadingService removeReadingService) {
        this.processCompleteInformationService = processCompleteInformationService;
        this.registerReadingService = registerReadingService;
        this.consultReadingsService = consultReadingsService;
        this.processAccumulatedService = processAccumulatedService;
        this.replaceReadingService = replaceReadingService;
        this.updateReadingService = updateReadingService;
        this.removeReadingService = removeReadingService;
    }

    @Override
    public AlchemyReport processCompleteInformation(List<DeviceReading> readings) {
        return processCompleteInformationService.process(readings);
    }

    @Override
    public DeviceReading registerReading(DeviceReading reading) {
        return registerReadingService.register(reading);
    }

    @Override
    public AccumulatedInformation consultReadings() {
        return consultReadingsService.consultAll();
    }

    @Override
    public DeviceReading consultReading(SurveillanceDevice device) {
        return consultReadingsService.consultOne(device);
    }

    @Override
    public AlchemyReport processAccumulatedInformation() {
        return processAccumulatedService.process();
    }

    @Override
    public DeviceReading replaceReading(DeviceReading reading) {
        return replaceReadingService.replace(reading);
    }

    @Override
    public DeviceReading updateReading(SurveillanceDevice device, ReadingUpdate update) {
        return updateReadingService.update(device, update);
    }

    @Override
    public void removeReading(SurveillanceDevice device) {
        removeReadingService.remove(device);
    }
}
