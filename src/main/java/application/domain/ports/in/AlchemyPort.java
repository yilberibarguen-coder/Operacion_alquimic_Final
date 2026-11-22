package application.domain.ports.in;

import application.domain.models.AccumulatedInformation;
import application.domain.models.AlchemyReport;
import application.domain.models.DeviceReading;
import application.domain.valueobjects.ReadingUpdate;
import application.domain.valueobjects.SurveillanceDevice;

import java.util.List;

/** What the application offers to the outside world, one operation per functional requirement. */
public interface AlchemyPort {

    /** F1 - Processes the complete information of the three devices received together (nothing is stored). */
    AlchemyReport processCompleteInformation(List<DeviceReading> readings);

    /** F2 - Registers the information of one device. Fails if the device already has one (use replace or update). */
    DeviceReading registerReading(DeviceReading reading);

    /** F3 - What has been registered and whether it is enough to process. */
    AccumulatedInformation consultReadings();

    DeviceReading consultReading(SurveillanceDevice device);

    /** F3 - Processes what has been accumulated; fails with "insufficient data" when it cannot. */
    AlchemyReport processAccumulatedInformation();

    /** F4 - Replaces completely the reading of a device. */
    DeviceReading replaceReading(DeviceReading reading);

    /** F5 - Updates only the distance and/or the fragment of a device. */
    DeviceReading updateReading(SurveillanceDevice device, ReadingUpdate update);

    /** F6 - Removes completely the reading of a device. */
    void removeReading(SurveillanceDevice device);
}
