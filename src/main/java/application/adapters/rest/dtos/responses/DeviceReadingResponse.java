package application.adapters.rest.dtos.responses;

import java.util.List;

public record DeviceReadingResponse(String deviceId, double distance, List<String> message) {
}
