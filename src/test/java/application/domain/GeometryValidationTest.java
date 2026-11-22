package application.domain;

import application.TestData;
import application.domain.exceptions.InconsistentDistancesException;
import application.domain.exceptions.InvalidInformationException;
import application.domain.services.LabLocator;
import application.domain.valueobjects.Distance;
import application.domain.valueobjects.SurveillanceDevice;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeometryValidationTest {
    @Test
    void rejectsImpossibleDistancesEvenWhenEveryValueIsPositive() {
        var distances = new EnumMap<SurveillanceDevice, Distance>(SurveillanceDevice.class);
        for (var device : SurveillanceDevice.values()) distances.put(device, new Distance(1));
        assertThrows(InconsistentDistancesException.class, () -> new LabLocator().locate(distances));
    }

    @Test
    void checksAllThreeDistancesRatherThanOnlyTheirDifferences() {
        var distances = new EnumMap<SurveillanceDevice, Distance>(SurveillanceDevice.class);
        // Adding the same squared error preserves the linearized solution, but not the circles.
        for (var device : SurveillanceDevice.values()) {
            var position = device.getPosition();
            double exact = Math.hypot(100 - position.x(), 100 - position.y());
            distances.put(device, new Distance(Math.sqrt(exact * exact + 10000)));
        }
        assertThrows(InconsistentDistancesException.class, () -> new LabLocator().locate(distances));
    }

    @Test
    void acceptsDistancesRoundedToTwoDecimalsForSeveralLocations() {
        for (double[] point : new double[][]{{100,100}, {0,0}, {-300.25,450.75}, {-500,-200}}) {
            var distances = new EnumMap<SurveillanceDevice, Distance>(SurveillanceDevice.class);
            for (var device : SurveillanceDevice.values()) {
                var position = device.getPosition();
                distances.put(device, new Distance(Math.round(Math.hypot(point[0] - position.x(),
                        point[1] - position.y()) * 100) / 100.0));
            }
            var result = new LabLocator().locate(distances);
            assertEquals(point[0], result.x(), 0.1);
            assertEquals(point[1], result.y(), 0.1);
        }
    }

    @Test
    void accumulatedInformationExplainsInconsistentGeometryAndCanBeCorrected() {
        var app = TestData.newApplication();
        app.registerReading(TestData.starLabs());
        app.registerReading(TestData.ccpd());
        app.registerReading(TestData.reading(SurveillanceDevice.STAR_SATELLITE, 1, TestData.SATELLITE_WORDS));
        var information = app.consultReadings();
        assertFalse(information.sufficient());
        assertTrue(information.missingDevices().isEmpty());
        assertTrue(information.reason().contains("distances"));
        assertThrows(InconsistentDistancesException.class, app::processAccumulatedInformation);
        app.replaceReading(TestData.satellite());
        assertTrue(app.consultReadings().sufficient());
        assertEquals(TestData.FULL_MESSAGE, app.processAccumulatedInformation().message());
    }

    @Test
    void inputPortRejectsNullElementsWithoutHttpValidation() {
        assertThrows(InvalidInformationException.class, () -> TestData.newApplication()
                .processCompleteInformation(Arrays.asList(TestData.starLabs(), null, TestData.satellite())));
    }
}
