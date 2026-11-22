package application.domain.valueobjects;

import application.domain.exceptions.InvalidUpdateException;

import java.util.Optional;

/** Partial update of a reading: the distance, the message fragment or both. What is absent is kept as it was. */
public final class ReadingUpdate {

    private final Distance distance;
    private final MessageFragment fragment;

    public ReadingUpdate(Distance distance, MessageFragment fragment) {
        if (distance == null && fragment == null) {
            throw new InvalidUpdateException("An update must provide the distance, the message fragment or both.");
        }
        this.distance = distance;
        this.fragment = fragment;
    }

    public Optional<Distance> distance() {
        return Optional.ofNullable(distance);
    }

    public Optional<MessageFragment> fragment() {
        return Optional.ofNullable(fragment);
    }
}
