package application.domain.models;

import application.domain.valueobjects.SurveillanceDevice;

import java.util.List;

/**
 * What has been registered so far (F3): the readings, the devices still missing and whether the information
 * is already enough to process. {@code reason} explains why it is not enough (null when it is).
 */
public record AccumulatedInformation(List<DeviceReading> readings, List<SurveillanceDevice> missingDevices,
                                     boolean sufficient, String reason) {
}
