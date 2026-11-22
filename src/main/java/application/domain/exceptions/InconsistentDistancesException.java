package application.domain.exceptions;

/** The distances cannot describe a common laboratory position within the accepted precision. */
public class InconsistentDistancesException extends DomainException {
    public InconsistentDistancesException(String message) { super(message); }
}
