package application.domain.exceptions;

public class ReadingAlreadyExistsException extends DomainException {
    public ReadingAlreadyExistsException(String message) {
        super(message);
    }
}
