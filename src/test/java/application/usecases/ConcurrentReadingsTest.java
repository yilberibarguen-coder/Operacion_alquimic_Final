package application.usecases;

import application.TestData;
import application.domain.exceptions.ReadingAlreadyExistsException;
import application.domain.exceptions.ReadingNotFoundException;
import application.domain.ports.in.AlchemyPort;
import application.domain.valueobjects.Distance;
import application.domain.valueobjects.MessageFragment;
import application.domain.valueobjects.ReadingUpdate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static application.domain.valueobjects.SurveillanceDevice.*;
import static org.junit.jupiter.api.Assertions.*;

class ConcurrentReadingsTest {
    private <T> List<T> simultaneously(List<Callable<T>> operations) throws Exception {
        var executor = Executors.newFixedThreadPool(operations.size());
        var ready = new CountDownLatch(operations.size());
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<T>>();
        try {
            for (var operation : operations) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(20, TimeUnit.SECONDS)) throw new TimeoutException("Start barrier timed out");
                    return operation.call();
                }));
            }
            assertTrue(ready.await(20, TimeUnit.SECONDS), "Workers did not reach the start barrier");
            start.countDown();
            var results = new ArrayList<T>();
            for (var future : futures) results.add(future.get(20, TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Workers did not stop");
        }
    }

    private AlchemyPort registeredApplication() {
        var app = TestData.newApplication();
        app.registerReading(TestData.starLabs());
        app.registerReading(TestData.ccpd());
        app.registerReading(TestData.satellite());
        return app;
    }

    @Test void duplicateConcurrentRegistrationsHaveExactlyOneWinner() throws Exception {
        var app = TestData.newApplication();
        var operations = new ArrayList<Callable<Boolean>>();
        for (int i = 0; i < 8; i++) {
            operations.add(() -> {
                try {
                    app.registerReading(TestData.ccpd());
                    return true;
                } catch (ReadingAlreadyExistsException expected) {
                    return false;
                }
            });
        }
        assertEquals(1L, simultaneously(operations).stream().filter(Boolean::booleanValue).count());
        assertEquals(1, app.consultReadings().readings().size());
        assertEquals(TestData.ccpd().getFragment(), app.consultReading(CCPD).getFragment());
    }

    @Test void concurrentPartialUpdatesPreserveBothFieldsAndOtherDevices() throws Exception {
        var app = registeredApplication();
        var words = new MessageFragment(List.of("nuevo"));
        for (int iteration = 0; iteration < 20; iteration++) {
            app.replaceReading(TestData.ccpd());
            simultaneously(List.of(
                    () -> app.updateReading(CCPD, new ReadingUpdate(new Distance(250), null)),
                    () -> app.updateReading(CCPD, new ReadingUpdate(null, words))));
            assertEquals(250.0, app.consultReading(CCPD).getDistance().value());
            assertEquals(words, app.consultReading(CCPD).getFragment());
            assertEquals(TestData.starLabs().getFragment(), app.consultReading(STAR_LABS).getFragment());
            assertEquals(TestData.satellite().getDistance(), app.consultReading(STAR_SATELLITE).getDistance());
        }
    }

    @Test void concurrentDeletionAndUpdateCannotResurrectAReading() throws Exception {
        var app = registeredApplication();
        for (int iteration = 0; iteration < 20; iteration++) {
            if (iteration > 0) app.registerReading(TestData.ccpd());
            simultaneously(List.<Callable<Boolean>>of(
                    () -> { app.removeReading(CCPD); return true; },
                    () -> {
                        try {
                            app.updateReading(CCPD, new ReadingUpdate(new Distance(250), null));
                        } catch (ReadingNotFoundException expected) {
                            // Deletion may win; a partial update must never recreate the reading.
                        }
                        return true;
                    }));
            assertThrows(ReadingNotFoundException.class, () -> app.consultReading(CCPD));
            assertEquals(2, app.consultReadings().readings().size());
            assertEquals(TestData.starLabs().getFragment(), app.consultReading(STAR_LABS).getFragment());
            assertEquals(TestData.satellite().getDistance(), app.consultReading(STAR_SATELLITE).getDistance());
        }
    }

    @Test void devicesCanBeRegisteredAtTheSameTimeAndThenProcessed() throws Exception {
        var app = TestData.newApplication();
        simultaneously(List.of(
                () -> app.registerReading(TestData.satellite()),
                () -> app.registerReading(TestData.starLabs()),
                () -> app.registerReading(TestData.ccpd())));
        assertTrue(app.consultReadings().sufficient());
        assertEquals(TestData.FULL_MESSAGE, app.processAccumulatedInformation().message());
    }
}
