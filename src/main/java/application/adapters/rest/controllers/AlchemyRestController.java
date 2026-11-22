package application.adapters.rest.controllers;

import application.adapters.rest.dtos.requests.AnalysisRequest;
import application.adapters.rest.dtos.requests.RegisterReadingRequest;
import application.adapters.rest.dtos.requests.ReplaceReadingRequest;
import application.adapters.rest.dtos.requests.UpdateReadingRequest;
import application.adapters.rest.dtos.responses.AccumulatedInformationResponse;
import application.adapters.rest.dtos.responses.AnalysisResponse;
import application.adapters.rest.dtos.responses.DeviceReadingResponse;
import application.adapters.rest.mappers.AlchemyRestMapper;
import application.domain.models.DeviceReading;
import application.domain.ports.in.AlchemyPort;
import application.domain.valueobjects.SurveillanceDevice;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HTTP adapter: translates requests into calls to the input port. It contains no business rules. */
@RestController
@RequestMapping("/api/v1")
public class AlchemyRestController {

    private final AlchemyPort alchemyPort;

    public AlchemyRestController(AlchemyPort alchemyPort) {
        this.alchemyPort = alchemyPort;
    }

    /** F1 - Processes the complete information of the three devices. */
    @PostMapping("/analysis")
    public ResponseEntity<AnalysisResponse> processComplete(@Valid @RequestBody AnalysisRequest request) {
        List<DeviceReading> readings = request.devices().stream().map(AlchemyRestMapper::toDomain).toList();
        return ResponseEntity.ok(AlchemyRestMapper.toResponse(alchemyPort.processCompleteInformation(readings)));
    }

    /** F3 - Processes the information accumulated so far. */
    @GetMapping("/analysis")
    public ResponseEntity<AnalysisResponse> processAccumulated() {
        return ResponseEntity.ok(AlchemyRestMapper.toResponse(alchemyPort.processAccumulatedInformation()));
    }

    /** F2 - Registers the reading of one device. */
    @PostMapping("/devices")
    public ResponseEntity<DeviceReadingResponse> register(@Valid @RequestBody RegisterReadingRequest request) {
        DeviceReading saved = alchemyPort.registerReading(AlchemyRestMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(AlchemyRestMapper.toResponse(saved));
    }

    /** F3 - What is registered and whether it is enough to process. */
    @GetMapping("/devices")
    public ResponseEntity<AccumulatedInformationResponse> consultAll() {
        return ResponseEntity.ok(AlchemyRestMapper.toResponse(alchemyPort.consultReadings()));
    }

    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<DeviceReadingResponse> consultOne(@PathVariable("deviceId") String deviceId) {
        DeviceReading reading = alchemyPort.consultReading(SurveillanceDevice.from(deviceId));
        return ResponseEntity.ok(AlchemyRestMapper.toResponse(reading));
    }

    /** F4 - Full replacement. */
    @PutMapping("/devices/{deviceId}")
    public ResponseEntity<DeviceReadingResponse> replace(@PathVariable("deviceId") String deviceId,
                                                         @Valid @RequestBody ReplaceReadingRequest request) {
        DeviceReading replacement = AlchemyRestMapper.toDomain(SurveillanceDevice.from(deviceId), request);
        return ResponseEntity.ok(AlchemyRestMapper.toResponse(alchemyPort.replaceReading(replacement)));
    }

    /** F5 - Partial update. */
    @PatchMapping("/devices/{deviceId}")
    public ResponseEntity<DeviceReadingResponse> update(@PathVariable("deviceId") String deviceId,
                                                        @RequestBody UpdateReadingRequest request) {
        DeviceReading updated = alchemyPort.updateReading(SurveillanceDevice.from(deviceId),
                AlchemyRestMapper.toDomain(request));
        return ResponseEntity.ok(AlchemyRestMapper.toResponse(updated));
    }

    /** F6 - Removal. */
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<Void> remove(@PathVariable("deviceId") String deviceId) {
        alchemyPort.removeReading(SurveillanceDevice.from(deviceId));
        return ResponseEntity.noContent().build();
    }
}
