package application.domain.valueobjects;

import application.domain.exceptions.InvalidMessageException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Part of the intercepted message: one word per position. A word that was not received is represented by "".
 * Immutable; words are trimmed, so a blank word counts as not received.
 */
public final class MessageFragment {

    private final List<String> words;

    public MessageFragment(List<String> rawWords) {
        if (rawWords == null || rawWords.isEmpty()) {
            throw new InvalidMessageException("The message fragment must have at least one position.");
        }
        List<String> normalized = new ArrayList<>(rawWords.size());
        for (String word : rawWords) {
            if (word == null) {
                throw new InvalidMessageException("A fragment cannot contain null words (use \"\" for a word that was not received).");
            }
            normalized.add(word.trim());
        }
        this.words = List.copyOf(normalized);
    }

    public int size() {
        return words.size();
    }

    public String wordAt(int index) {
        return words.get(index);
    }

    public boolean isReceivedAt(int index) {
        return !words.get(index).isEmpty();
    }

    public List<String> words() {
        return words;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MessageFragment other && words.equals(other.words);
    }

    @Override
    public int hashCode() {
        return Objects.hash(words);
    }
}
