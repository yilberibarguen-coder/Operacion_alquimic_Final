package application;

import application.adapters.persistence.memory.InMemoryReadingRepositoryAdapter;
import application.adapters.useCases.AlchemyUseCaseImpl;
import application.domain.models.DeviceReading;
import application.domain.ports.in.AlchemyPort;
import application.domain.services.AlchemyAnalyzer;
import application.domain.services.ConsultReadingsService;
import application.domain.services.ProcessAccumulatedService;
import application.domain.services.ProcessCompleteInformationService;
import application.domain.services.RegisterReadingService;
import application.domain.services.RemoveReadingService;
import application.domain.services.ReplaceReadingService;
import application.domain.services.UpdateReadingService;
import application.domain.valueobjects.Distance;
import application.domain.valueobjects.MessageFragment;
import application.domain.valueobjects.SurveillanceDevice;

import java.util.List;

/** Shared test helpers. The laboratory used in the examples is at (100, 100). */
public final class TestData {

    private TestData() {}

    public static final double STAR_LABS_DISTANCE = Math.hypot(600, 300);   // 670.82...
    public static final double CCPD_DISTANCE = 200;
    public static final double SATELLITE_DISTANCE = 400;

    public static final List<String> STAR_LABS_WORDS = List.of("este", "", "", "mensaje", "");
    public static final List<String> CCPD_WORDS = List.of("", "es", "un", "", "");
    public static final List<String> SATELLITE_WORDS = List.of("este", "", "", "", "secreto");
    public static final String FULL_MESSAGE = "este es un mensaje secreto";

    public static DeviceReading reading(SurveillanceDevice device, double distance, List<String> words) {
        return new DeviceReading(device, new Distance(distance), new MessageFragment(words));
    }

    public static DeviceReading starLabs() { return reading(SurveillanceDevice.STAR_LABS, STAR_LABS_DISTANCE, STAR_LABS_WORDS); }
    public static DeviceReading ccpd() { return reading(SurveillanceDevice.CCPD, CCPD_DISTANCE, CCPD_WORDS); }
    public static DeviceReading satellite() { return reading(SurveillanceDevice.STAR_SATELLITE, SATELLITE_DISTANCE, SATELLITE_WORDS); }

    /** Wires the application the way Spring does, over the in-memory adapter. */
    public static AlchemyPort newApplication() {
        InMemoryReadingRepositoryAdapter repository = new InMemoryReadingRepositoryAdapter();
        AlchemyAnalyzer analyzer = new AlchemyAnalyzer();
        return new AlchemyUseCaseImpl(
                new ProcessCompleteInformationService(analyzer),
                new RegisterReadingService(repository),
                new ConsultReadingsService(repository, analyzer),
                new ProcessAccumulatedService(repository, analyzer),
                new ReplaceReadingService(repository),
                new UpdateReadingService(repository),
                new RemoveReadingService(repository));
    }
}
