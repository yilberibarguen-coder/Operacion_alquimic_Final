package application.domain.exceptions;

public class UnknownDeviceException extends DomainException {
    public UnknownDeviceException(String message) {
        super(message);
    }
}
