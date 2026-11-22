package application.adapters;

import application.TestData;
import application.adapters.rest.controllers.AlchemyRestController;
import application.adapters.rest.dtos.requests.AnalysisRequest;
import application.adapters.rest.dtos.requests.RegisterReadingRequest;
import application.adapters.rest.dtos.requests.ReplaceReadingRequest;
import application.adapters.rest.dtos.requests.UpdateReadingRequest;
import application.adapters.rest.dtos.responses.AccumulatedInformationResponse;
import application.adapters.rest.dtos.responses.AnalysisResponse;
import application.adapters.rest.dtos.responses.DeviceReadingResponse;
import application.adapters.rest.exception.RestExceptionHandler;
import application.adapters.rest.mappers.AlchemyRestMapper;
import application.domain.exceptions.DomainException;
import application.domain.exceptions.InconsistentMessageException;
import application.domain.exceptions.InsufficientDataException;
import application.domain.exceptions.InvalidDistanceException;
import application.domain.exceptions.InvalidMessageException;
import application.domain.exceptions.InvalidUpdateException;
import application.domain.exceptions.ReadingAlreadyExistsException;
import application.domain.exceptions.ReadingNotFoundException;
import application.domain.exceptions.UnknownDeviceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the HTTP adapter without starting a server: controller, mapper and error translation. */
class RestAdapterTest {

    private final AlchemyRestController controller = new AlchemyRestController(TestData.newApplication());
    private final RestExceptionHandler handler = new RestExceptionHandler();

    private RegisterReadingRequest request(String deviceId, double distance, List<String> words) {
        return new RegisterReadingRequest(deviceId, distance, words);
    }

    private void registerAllThroughHttp() {
        controller.register(request("STAR_LABS", TestData.STAR_LABS_DISTANCE, TestData.STAR_LABS_WORDS));
        controller.register(request("ccpd", TestData.CCPD_DISTANCE, TestData.CCPD_WORDS));
        controller.register(request("S.T.A.R. Satellite", TestData.SATELLITE_DISTANCE, TestData.SATELLITE_WORDS));
    }

    @Test
    void registerReturns201WithTheStoredReading() {
        ResponseEntity<DeviceReadingResponse> response =
                controller.register(request("STAR_LABS", 670.82, TestData.STAR_LABS_WORDS));
        assertEquals(201, response.getStatusCode().value());
        assertEquals("STAR_LABS", response.getBody().deviceId());
        assertEquals(TestData.STAR_LABS_WORDS, response.getBody().message());
    }

    @Test
    void processCompleteReturnsPositionAndMessage() {
        AnalysisRequest body = new AnalysisRequest(List.of(
                request("STAR_LABS", TestData.STAR_LABS_DISTANCE, TestData.STAR_LABS_WORDS),
                request("CCPD", TestData.CCPD_DISTANCE, TestData.CCPD_WORDS),
                request("STAR_SATELLITE", TestData.SATELLITE_DISTANCE, TestData.SATELLITE_WORDS)));
        ResponseEntity<AnalysisResponse> response = controller.processComplete(body);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(100.0, response.getBody().position().x());
        assertEquals(100.0, response.getBody().position().y());
        assertEquals(TestData.FULL_MESSAGE, response.getBody().message());
    }

    @Test
    void accumulatedProcessingAndConsultationThroughHttp() {
        controller.register(request("CCPD", TestData.CCPD_DISTANCE, TestData.CCPD_WORDS));
        ResponseEntity<AccumulatedInformationResponse> partial = controller.consultAll();
        assertEquals(false, partial.getBody().sufficientData());
        assertEquals(List.of("STAR_LABS", "STAR_SATELLITE"), partial.getBody().missingDevices());
        assertThrows(InsufficientDataException.class, () -> controller.processAccumulated());

        controller.register(request("STAR_LABS", TestData.STAR_LABS_DISTANCE, TestData.STAR_LABS_WORDS));
        controller.register(request("STAR_SATELLITE", TestData.SATELLITE_DISTANCE, TestData.SATELLITE_WORDS));
        assertEquals(true, controller.consultAll().getBody().sufficientData());
        assertEquals(TestData.FULL_MESSAGE, controller.processAccumulated().getBody().message());
    }

    @Test
    void putReplacesPatchUpdatesDeleteRemoves() {
        registerAllThroughHttp();

        ResponseEntity<DeviceReadingResponse> replaced =
                controller.replace("ccpd", new ReplaceReadingRequest(300.0, List.of("", "fue", "un", "", "")));
        assertEquals(200, replaced.getStatusCode().value());
        assertEquals(300.0, replaced.getBody().distance());

        ResponseEntity<DeviceReadingResponse> patched = controller.update("ccpd", new UpdateReadingRequest(250.0, null));
        assertEquals(250.0, patched.getBody().distance());
        assertEquals(List.of("", "fue", "un", "", ""), patched.getBody().message());

        ResponseEntity<Void> removed = controller.remove("CCPD");
        assertEquals(204, removed.getStatusCode().value());
        assertThrows(ReadingNotFoundException.class, () -> controller.consultOne("CCPD"));
        assertEquals("STAR_LABS", controller.consultOne("star labs").getBody().deviceId());
    }

    @Test
    void unknownDeviceInThePathIsRejected() {
        assertThrows(UnknownDeviceException.class, () -> controller.consultOne("batcave"));
        assertThrows(UnknownDeviceException.class, () -> controller.remove("batcave"));
    }

    @Test
    void mapperTreatsMissingFieldsOfAPatchAsAbsentButRejectsInvalidOnes() {
        assertThrows(InvalidUpdateException.class, () -> AlchemyRestMapper.toDomain(new UpdateReadingRequest(null, null)));
        assertThrows(InvalidMessageException.class, () -> AlchemyRestMapper.toDomain(new UpdateReadingRequest(null, List.of())));
        assertThrows(InvalidDistanceException.class, () -> AlchemyRestMapper.toDomain(new UpdateReadingRequest(-5.0, null)));
    }

    @Test
    void errorsAreTranslatedToTheRightHttpStatus() {
        assertEquals(404, status(new ReadingNotFoundException("x")));
        assertEquals(409, status(new ReadingAlreadyExistsException("x")));
        assertEquals(422, status(new InsufficientDataException("x")));
        assertEquals(422, status(new InconsistentMessageException("x")));
        assertEquals(400, status(new UnknownDeviceException("x")));
        assertEquals(400, status(new InvalidDistanceException("x")));
        assertEquals(400, status(new InvalidMessageException("x")));
        assertEquals(400, status(new InvalidUpdateException("x")));
        ResponseEntity<?> response = handler.handleDomain(new ReadingNotFoundException("no reading"));
        assertTrue(response.getBody().toString().contains("no reading"));
    }

    private int status(DomainException exception) {
        return handler.handleDomain(exception).getStatusCode().value();
    }
}
