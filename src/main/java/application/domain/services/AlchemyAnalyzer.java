package application.domain.services;

import application.domain.exceptions.InsufficientDataException;
import application.domain.exceptions.InvalidInformationException;
import application.domain.models.AlchemyReport;
import application.domain.models.DeviceReading;
import application.domain.valueobjects.Distance;
import application.domain.valueobjects.MessageFragment;
import application.domain.valueobjects.SurveillanceDevice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Combines the readings of the devices: locates the laboratory and rebuilds the message (F1 and F3). */
public class AlchemyAnalyzer {

    private final LabLocator labLocator = new LabLocator();
    private final MessageReconstructor messageReconstructor = new MessageReconstructor();

    public AlchemyReport analyze(Collection<DeviceReading> readings) {
        if (readings == null || readings.stream().anyMatch(java.util.Objects::isNull)) {
            throw new InvalidInformationException("Readings and their elements must not be null.");
        }
        Map<SurveillanceDevice, DeviceReading> byDevice = new EnumMap<>(SurveillanceDevice.class);
        for (DeviceReading reading : readings) {
            if (byDevice.put(reading.getDevice(), reading) != null) {
                throw new InvalidInformationException("The device " + reading.getDevice() + " appears more than once.");
            }
        }
        List<SurveillanceDevice> missing = missingDevices(byDevice.keySet());
        if (!missing.isEmpty()) {
            throw new InsufficientDataException("There is not enough information: missing " + missing + ".");
        }
        Map<SurveillanceDevice, Distance> distances = new EnumMap<>(SurveillanceDevice.class);
        List<MessageFragment> fragments = new ArrayList<>();
        for (SurveillanceDevice device : SurveillanceDevice.values()) {
            distances.put(device, byDevice.get(device).getDistance());
            fragments.add(byDevice.get(device).getFragment());
        }
        return new AlchemyReport(labLocator.locate(distances), messageReconstructor.reconstruct(fragments));
    }

    public List<SurveillanceDevice> missingDevices(Collection<SurveillanceDevice> present) {
        List<SurveillanceDevice> missing = new ArrayList<>();
        for (SurveillanceDevice device : SurveillanceDevice.values()) {
            if (!present.contains(device)) {
                missing.add(device);
            }
        }
        return missing;
    }
}
