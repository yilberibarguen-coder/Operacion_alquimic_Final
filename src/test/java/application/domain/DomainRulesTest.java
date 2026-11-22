package application.domain;

import application.TestData;
import application.domain.exceptions.InconsistentMessageException;
import application.domain.exceptions.InsufficientDataException;
import application.domain.exceptions.InvalidDistanceException;
import application.domain.exceptions.InvalidInformationException;
import application.domain.exceptions.InvalidMessageException;
import application.domain.exceptions.InvalidUpdateException;
import application.domain.exceptions.UnknownDeviceException;
import application.domain.models.DeviceReading;
import application.domain.services.LabLocator;
import application.domain.services.MessageReconstructor;
import application.domain.valueobjects.Distance;
import application.domain.valueobjects.MessageFragment;
import application.domain.valueobjects.Position;
import application.domain.valueobjects.ReadingUpdate;
import application.domain.valueobjects.SurveillanceDevice;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainRulesTest {

    private final LabLocator locator = new LabLocator();
    private final MessageReconstructor reconstructor = new MessageReconstructor();

    private Map<SurveillanceDevice, Distance> distancesTo(double x, double y) {
        Map<SurveillanceDevice, Distance> distances = new EnumMap<>(SurveillanceDevice.class);
        for (SurveillanceDevice device : SurveillanceDevice.values()) {
            Position p = device.getPosition();
            distances.put(device, new Distance(Math.hypot(x - p.x(), y - p.y())));
        }
        return distances;
    }

    private MessageFragment fragment(String... words) {
        return new MessageFragment(Arrays.asList(words));
    }

    // ---------- trilateration ----------

    @Test
    void trilaterationFindsTheLaboratoryFromExactDistances() {
        double[][] laboratories = {{100, 100}, {-100, 75.5}, {0, 0}, {-300.25, 450.75}, {1000, -1000}, {-500, -200}};
        for (double[] lab : laboratories) {
            assertEquals(new Position(lab[0], lab[1]), locator.locate(distancesTo(lab[0], lab[1])));
        }
    }

    @Test
    void trilaterationUsesTheFixedPositionsOfTheDevices() {
        assertEquals(new Position(-500, -200), SurveillanceDevice.STAR_LABS.getPosition());
        assertEquals(new Position(100, -100), SurveillanceDevice.CCPD.getPosition());
        assertEquals(new Position(500, 100), SurveillanceDevice.STAR_SATELLITE.getPosition());
    }

    @Test
    void trilaterationToleratesRoundedDistancesAndReportsTwoDecimals() {
        Map<SurveillanceDevice, Distance> distances = new EnumMap<>(SurveillanceDevice.class);
        distances.put(SurveillanceDevice.STAR_LABS, new Distance(670.82));
        distances.put(SurveillanceDevice.CCPD, new Distance(200));
        distances.put(SurveillanceDevice.STAR_SATELLITE, new Distance(400));
        Position located = locator.locate(distances);
        assertTrue(Math.abs(located.x() - 100) < 0.1 && Math.abs(located.y() - 100) < 0.1);
        assertEquals(located, located.rounded());
    }

    // ---------- message reconstruction ----------

    @Test
    void reconstructionIgnoresEmptyWordsAndMergesTheFragments() {
        String message = reconstructor.reconstruct(List.of(
                new MessageFragment(TestData.STAR_LABS_WORDS),
                new MessageFragment(TestData.CCPD_WORDS),
                new MessageFragment(TestData.SATELLITE_WORDS)));
        assertEquals(TestData.FULL_MESSAGE, message);
    }

    @Test
    void reconstructionIsIndependentOfTheOrderOfTheFragments() {
        MessageFragment a = new MessageFragment(TestData.STAR_LABS_WORDS);
        MessageFragment b = new MessageFragment(TestData.CCPD_WORDS);
        MessageFragment c = new MessageFragment(TestData.SATELLITE_WORDS);
        assertEquals(reconstructor.reconstruct(List.of(a, b, c)), reconstructor.reconstruct(List.of(c, a, b)));
    }

    @Test
    void reconstructionFailsWhenAWordWasNotReceivedByAnyDevice() {
        assertThrows(InsufficientDataException.class,
                () -> reconstructor.reconstruct(List.of(fragment("hola", ""), fragment("hola", ""), fragment("", ""))));
    }

    @Test
    void reconstructionFailsWhenFragmentsDisagreeOrHaveDifferentLengths() {
        assertThrows(InconsistentMessageException.class,
                () -> reconstructor.reconstruct(List.of(fragment("hola"), fragment("adios"))));
        assertThrows(InconsistentMessageException.class,
                () -> reconstructor.reconstruct(List.of(fragment("hola", ""), fragment("hola"))));
    }

    @Test
    void sameWordReceivedByTwoDevicesIsNotAConflict() {
        assertEquals("hola mundo", reconstructor.reconstruct(List.of(fragment("hola", "mundo"), fragment("hola", ""))));
    }

    // ---------- value objects ----------

    @Test
    void deviceIdentificationIsAcceptedInSeveralForms() {
        assertEquals(SurveillanceDevice.STAR_LABS, SurveillanceDevice.from("STAR_LABS"));
        assertEquals(SurveillanceDevice.STAR_LABS, SurveillanceDevice.from("star-labs"));
        assertEquals(SurveillanceDevice.STAR_LABS, SurveillanceDevice.from(" STAR Labs "));
        assertEquals(SurveillanceDevice.CCPD, SurveillanceDevice.from("ccpd"));
        assertEquals(SurveillanceDevice.STAR_SATELLITE, SurveillanceDevice.from("S.T.A.R. Satellite"));
        assertThrows(UnknownDeviceException.class, () -> SurveillanceDevice.from("batcave"));
        assertThrows(UnknownDeviceException.class, () -> SurveillanceDevice.from(" "));
        assertThrows(UnknownDeviceException.class, () -> SurveillanceDevice.from(null));
    }

    @Test
    void distanceMustBeFiniteAndNotNegative() {
        new Distance(0);
        new Distance(123.45);
        assertThrows(InvalidDistanceException.class, () -> new Distance(-0.1));
        assertThrows(InvalidDistanceException.class, () -> new Distance(Double.NaN));
        assertThrows(InvalidDistanceException.class, () -> new Distance(Double.POSITIVE_INFINITY));
    }

    @Test
    void fragmentNeedsPositionsAndRejectsNullWords() {
        assertThrows(InvalidMessageException.class, () -> new MessageFragment(null));
        assertThrows(InvalidMessageException.class, () -> new MessageFragment(List.of()));
        assertThrows(InvalidMessageException.class, () -> new MessageFragment(Arrays.asList("hola", null)));
        MessageFragment withBlank = new MessageFragment(List.of("hola", "   "));
        assertTrue(withBlank.isReceivedAt(0));
        assertFalse(withBlank.isReceivedAt(1));
    }

    @Test
    void positionRejectsNonFiniteCoordinatesAndDoesNotReportNegativeZero() {
        assertThrows(InvalidInformationException.class, () -> new Position(Double.NaN, 0));
        assertEquals(new Position(0.0, 0.0), new Position(-0.001, 0.001).rounded());
    }

    // ---------- reading: replacement and partial update ----------

    @Test
    void anUpdateMustProvideSomething() {
        assertThrows(InvalidUpdateException.class, () -> new ReadingUpdate(null, null));
    }

    @Test
    void partialUpdateKeepsWhatIsNotProvided() {
        DeviceReading original = TestData.starLabs();
        DeviceReading onlyDistance = original.applyUpdate(new ReadingUpdate(new Distance(10), null));
        assertEquals(10.0, onlyDistance.getDistance().value());
        assertEquals(original.getFragment(), onlyDistance.getFragment());

        DeviceReading onlyFragment = original.applyUpdate(new ReadingUpdate(null, fragment("a", "b")));
        assertEquals(original.getDistance(), onlyFragment.getDistance());
        assertEquals(List.of("a", "b"), onlyFragment.getFragment().words());

        DeviceReading both = original.applyUpdate(new ReadingUpdate(new Distance(5), fragment("x")));
        assertEquals(5.0, both.getDistance().value());
        assertEquals(List.of("x"), both.getFragment().words());
        assertEquals(SurveillanceDevice.STAR_LABS, both.getDevice());
    }

    @Test
    void replacementMustBelongToTheSameDevice() {
        assertThrows(InvalidInformationException.class, () -> TestData.starLabs().replacedBy(TestData.ccpd()));
        DeviceReading replacement = TestData.reading(SurveillanceDevice.STAR_LABS, 1, List.of("z"));
        assertEquals(replacement, TestData.starLabs().replacedBy(replacement));
    }
}
