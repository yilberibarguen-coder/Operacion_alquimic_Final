package application.usecases;

import application.TestData;
import application.domain.exceptions.InconsistentMessageException;
import application.domain.exceptions.InsufficientDataException;
import application.domain.exceptions.InvalidInformationException;
import application.domain.exceptions.ReadingAlreadyExistsException;
import application.domain.exceptions.ReadingNotFoundException;
import application.domain.models.AccumulatedInformation;
import application.domain.models.AlchemyReport;
import application.domain.models.DeviceReading;
import application.domain.ports.in.AlchemyPort;
import application.domain.valueobjects.Distance;
import application.domain.valueobjects.MessageFragment;
import application.domain.valueobjects.Position;
import application.domain.valueobjects.ReadingUpdate;
import application.domain.valueobjects.SurveillanceDevice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static application.domain.valueobjects.SurveillanceDevice.CCPD;
import static application.domain.valueobjects.SurveillanceDevice.STAR_LABS;
import static application.domain.valueobjects.SurveillanceDevice.STAR_SATELLITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One test per functional requirement (F1 to F6) plus the consistency rules, through the input port. */
class AlchemyUseCasesTest {

    private final AlchemyPort app = TestData.newApplication();

    private void registerAll() {
        app.registerReading(TestData.starLabs());
        app.registerReading(TestData.ccpd());
        app.registerReading(TestData.satellite());
    }

    // ---------- F1 ----------

    @Test
    void f1ProcessesTheCompleteInformationOfTheThreeDevices() {
        AlchemyReport report = app.processCompleteInformation(List.of(TestData.starLabs(), TestData.ccpd(), TestData.satellite()));
        assertEquals(new Position(100, 100), report.position());
        assertEquals(TestData.FULL_MESSAGE, report.message());
    }

    @Test
    void f1DoesNotStoreAnything() {
        app.processCompleteInformation(List.of(TestData.starLabs(), TestData.ccpd(), TestData.satellite()));
        assertTrue(app.consultReadings().readings().isEmpty());
    }

    @Test
    void f1RequiresExactlyThreeDifferentDevices() {
        assertThrows(InvalidInformationException.class, () -> app.processCompleteInformation(List.of(TestData.starLabs(), TestData.ccpd())));
        assertThrows(InvalidInformationException.class, () -> app.processCompleteInformation(null));
        assertThrows(InvalidInformationException.class,
                () -> app.processCompleteInformation(List.of(TestData.starLabs(), TestData.starLabs(), TestData.ccpd())));
    }

    // ---------- F2 ----------

    @Test
    void f2RegistersEachDeviceIndependentlyAndInAnyOrder() {
        app.registerReading(TestData.satellite());
        app.registerReading(TestData.starLabs());
        assertEquals(2, app.consultReadings().readings().size());
        assertEquals(TestData.CCPD_DISTANCE, TestData.ccpd().getDistance().value());
        app.registerReading(TestData.ccpd());
        assertEquals(3, app.consultReadings().readings().size());
    }

    @Test
    void f2TheResultDoesNotDependOnTheRegistrationOrder() {
        AlchemyPort other = TestData.newApplication();
        registerAll();
        other.registerReading(TestData.ccpd());
        other.registerReading(TestData.satellite());
        other.registerReading(TestData.starLabs());
        AlchemyReport first = app.processAccumulatedInformation();
        AlchemyReport second = other.processAccumulatedInformation();
        assertEquals(first, second);
    }

    @Test
    void f2RegisteringAnExistingDeviceIsRejectedAndKeepsTheStoredData() {
        app.registerReading(TestData.starLabs());
        DeviceReading other = TestData.reading(STAR_LABS, 1, List.of("otro"));
        assertThrows(ReadingAlreadyExistsException.class, () -> app.registerReading(other));
        assertEquals(TestData.starLabs().getFragment(), app.consultReading(STAR_LABS).getFragment());
    }

    // ---------- F3 ----------

    @Test
    void f3ConsultsWhatIsRegisteredAndWhatIsMissing() {
        app.registerReading(TestData.ccpd());
        AccumulatedInformation information = app.consultReadings();
        assertEquals(1, information.readings().size());
        assertEquals(List.of(STAR_LABS, STAR_SATELLITE), information.missingDevices());
        assertFalse(information.sufficient());
        assertNotNull(information.reason());
        assertEquals(TestData.ccpd().getDistance(), app.consultReading(CCPD).getDistance());
    }

    @Test
    void f3ProcessesTheAccumulatedInformationWhenItIsEnough() {
        registerAll();
        AccumulatedInformation information = app.consultReadings();
        assertTrue(information.sufficient());
        assertTrue(information.missingDevices().isEmpty());
        AlchemyReport report = app.processAccumulatedInformation();
        assertEquals(new Position(100, 100), report.position());
        assertEquals(TestData.FULL_MESSAGE, report.message());
    }

    @Test
    void f3ReportsInsufficientDataWhenDevicesAreMissing() {
        assertThrows(InsufficientDataException.class, () -> app.processAccumulatedInformation());
        app.registerReading(TestData.starLabs());
        app.registerReading(TestData.ccpd());
        assertThrows(InsufficientDataException.class, () -> app.processAccumulatedInformation());
    }

    @Test
    void f3ReportsInsufficientDataWhenAWordIsMissingEverywhere() {
        app.registerReading(TestData.reading(STAR_LABS, TestData.STAR_LABS_DISTANCE, List.of("hola", "")));
        app.registerReading(TestData.reading(CCPD, TestData.CCPD_DISTANCE, List.of("hola", "")));
        app.registerReading(TestData.reading(STAR_SATELLITE, TestData.SATELLITE_DISTANCE, List.of("", "")));
        assertFalse(app.consultReadings().sufficient());
        assertThrows(InsufficientDataException.class, () -> app.processAccumulatedInformation());
    }

    @Test
    void f3ReportsInconsistentFragments() {
        app.registerReading(TestData.reading(STAR_LABS, TestData.STAR_LABS_DISTANCE, List.of("hola")));
        app.registerReading(TestData.reading(CCPD, TestData.CCPD_DISTANCE, List.of("adios")));
        app.registerReading(TestData.reading(STAR_SATELLITE, TestData.SATELLITE_DISTANCE, List.of("")));
        assertFalse(app.consultReadings().sufficient());
        assertThrows(InconsistentMessageException.class, () -> app.processAccumulatedInformation());
    }

    @Test
    void f3ConsultingAnUnregisteredDeviceFails() {
        assertThrows(ReadingNotFoundException.class, () -> app.consultReading(CCPD));
    }

    // ---------- F4 ----------

    @Test
    void f4ReplacesCompletelyTheReadingOfOneDeviceWithoutTouchingTheOthers() {
        registerAll();
        DeviceReading replacement = TestData.reading(CCPD, 300, List.of("", "", "", "", ""));
        app.replaceReading(replacement);
        assertEquals(300.0, app.consultReading(CCPD).getDistance().value());
        assertEquals(replacement.getFragment(), app.consultReading(CCPD).getFragment());
        assertEquals(TestData.starLabs().getFragment(), app.consultReading(STAR_LABS).getFragment());
        assertEquals(TestData.satellite().getDistance(), app.consultReading(STAR_SATELLITE).getDistance());
    }

    @Test
    void f4TheNextProcessingUsesTheReplacedInformation() {
        registerAll();
        app.replaceReading(TestData.reading(CCPD, TestData.CCPD_DISTANCE, List.of("", "fue", "un", "", "")));
        assertEquals("este fue un mensaje secreto", app.processAccumulatedInformation().message());
    }

    @Test
    void f4ReplacingAnUnregisteredDeviceFails() {
        assertThrows(ReadingNotFoundException.class, () -> app.replaceReading(TestData.ccpd()));
        assertTrue(app.consultReadings().readings().isEmpty());
    }

    // ---------- F5 ----------

    @Test
    void f5UpdatesOnlyTheDistanceAndKeepsTheFragment() {
        registerAll();
        app.updateReading(CCPD, new ReadingUpdate(new Distance(250), null));
        assertEquals(250.0, app.consultReading(CCPD).getDistance().value());
        assertEquals(TestData.ccpd().getFragment(), app.consultReading(CCPD).getFragment());
    }

    @Test
    void f5UpdatesOnlyTheFragmentAndKeepsTheDistance() {
        registerAll();
        app.updateReading(CCPD, new ReadingUpdate(null, new MessageFragment(List.of("", "fue", "un", "", ""))));
        assertEquals(TestData.CCPD_DISTANCE, app.consultReading(CCPD).getDistance().value());
        assertEquals(List.of("", "fue", "un", "", ""), app.consultReading(CCPD).getFragment().words());
    }

    @Test
    void f5UpdatesBothAndLeavesTheOtherDevicesUntouched() {
        registerAll();
        app.updateReading(STAR_LABS, new ReadingUpdate(new Distance(1), new MessageFragment(List.of("a"))));
        assertEquals(1.0, app.consultReading(STAR_LABS).getDistance().value());
        assertEquals(List.of("a"), app.consultReading(STAR_LABS).getFragment().words());
        assertEquals(TestData.ccpd().getFragment(), app.consultReading(CCPD).getFragment());
        assertEquals(TestData.satellite().getDistance(), app.consultReading(STAR_SATELLITE).getDistance());
    }

    @Test
    void f5IsNotAFullReplacement() {
        registerAll();
        app.updateReading(CCPD, new ReadingUpdate(new Distance(250), null));
        assertFalse(app.consultReading(CCPD).getFragment().words().isEmpty());
    }

    @Test
    void f5UpdatingAnUnregisteredDeviceFails() {
        assertThrows(ReadingNotFoundException.class, () -> app.updateReading(CCPD, new ReadingUpdate(new Distance(1), null)));
    }

    // ---------- F6 ----------

    @Test
    void f6RemovesOnlyTheSelectedDevice() {
        registerAll();
        app.removeReading(CCPD);
        assertThrows(ReadingNotFoundException.class, () -> app.consultReading(CCPD));
        assertEquals(2, app.consultReadings().readings().size());
        assertEquals(List.of(CCPD), app.consultReadings().missingDevices());
        assertThrows(InsufficientDataException.class, () -> app.processAccumulatedInformation());
    }

    @Test
    void f6ARemovedDeviceCanBeRegisteredAgain() {
        registerAll();
        app.removeReading(CCPD);
        app.registerReading(TestData.ccpd());
        assertEquals(TestData.FULL_MESSAGE, app.processAccumulatedInformation().message());
    }

    @Test
    void f6RemovingAnUnregisteredDeviceFails() {
        assertThrows(ReadingNotFoundException.class, () -> app.removeReading(STAR_LABS));
    }

    @Test
    void readingsAreAlwaysListedInTheFixedOrderOfTheDevices() {
        app.registerReading(TestData.satellite());
        app.registerReading(TestData.starLabs());
        app.registerReading(TestData.ccpd());
        List<SurveillanceDevice> order = app.consultReadings().readings().stream().map(DeviceReading::getDevice).toList();
        assertEquals(List.of(STAR_LABS, CCPD, STAR_SATELLITE), order);
    }
}
