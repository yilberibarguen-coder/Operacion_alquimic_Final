package application.adapters.rest.dtos.responses;

import java.util.List;

/** What is registered, what is missing and whether it is enough to process. */
public record AccumulatedInformationResponse(
        List<DeviceReadingResponse> readings,
        List<String> missingDevices,
        boolean sufficientData,
        String reason) {
}
