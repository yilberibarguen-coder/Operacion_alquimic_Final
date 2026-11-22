package application.domain.exceptions;

public class ReadingNotFoundException extends DomainException {
    public ReadingNotFoundException(String message) {
        super(message);
    }
}
