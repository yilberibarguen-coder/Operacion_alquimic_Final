package application.adapters.rest.mappers;

import application.adapters.rest.dtos.requests.RegisterReadingRequest;
import application.adapters.rest.dtos.requests.ReplaceReadingRequest;
import application.adapters.rest.dtos.requests.UpdateReadingRequest;
import application.adapters.rest.dtos.responses.AccumulatedInformationResponse;
import application.adapters.rest.dtos.responses.AnalysisResponse;
import application.adapters.rest.dtos.responses.DeviceReadingResponse;
import application.adapters.rest.dtos.responses.PositionResponse;
import application.domain.models.AccumulatedInformation;
import application.domain.models.AlchemyReport;
import application.domain.models.DeviceReading;
import application.domain.valueobjects.Distance;
import application.domain.valueobjects.MessageFragment;
import application.domain.valueobjects.ReadingUpdate;
import application.domain.valueobjects.SurveillanceDevice;

/** Translates between the JSON contract and the domain objects. */
public final class AlchemyRestMapper {

    private AlchemyRestMapper() {}

    public static DeviceReading toDomain(RegisterReadingRequest request) {
        return new DeviceReading(SurveillanceDevice.from(request.deviceId()),
                new Distance(request.distance()), new MessageFragment(request.message()));
    }

    public static DeviceReading toDomain(SurveillanceDevice device, ReplaceReadingRequest request) {
        return new DeviceReading(device, new Distance(request.distance()), new MessageFragment(request.message()));
    }

    public static ReadingUpdate toDomain(UpdateReadingRequest request) {
        Distance distance = request.distance() == null ? null : new Distance(request.distance());
        MessageFragment fragment = request.message() == null ? null : new MessageFragment(request.message());
        return new ReadingUpdate(distance, fragment);
    }

    public static DeviceReadingResponse toResponse(DeviceReading reading) {
        return new DeviceReadingResponse(reading.getDevice().name(), reading.getDistance().value(),
                reading.getFragment().words());
    }

    public static AnalysisResponse toResponse(AlchemyReport report) {
        return new AnalysisResponse(new PositionResponse(report.position().x(), report.position().y()), report.message());
    }

    public static AccumulatedInformationResponse toResponse(AccumulatedInformation information) {
        return new AccumulatedInformationResponse(
                information.readings().stream().map(AlchemyRestMapper::toResponse).toList(),
                information.missingDevices().stream().map(SurveillanceDevice::name).toList(),
                information.sufficient(), information.reason());
    }
}
