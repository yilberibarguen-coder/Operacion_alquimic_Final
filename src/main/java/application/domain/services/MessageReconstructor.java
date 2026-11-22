package application.domain.services;

import application.domain.exceptions.InconsistentMessageException;
import application.domain.exceptions.InsufficientDataException;
import application.domain.valueobjects.MessageFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Domain service: rebuilds the original message from the fragments. Words that were not received ("") are ignored;
 * for each position the word received by any device is used.
 */
public final class MessageReconstructor {

    public String reconstruct(Collection<MessageFragment> fragments) {
        if (fragments.isEmpty()) {
            throw new InsufficientDataException("There are no message fragments to reconstruct the message.");
        }
        int length = fragments.iterator().next().size();
        for (MessageFragment fragment : fragments) {
            if (fragment.size() != length) {
                throw new InconsistentMessageException("All the fragments must have the same number of positions.");
            }
        }
        List<String> words = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            String chosen = null;
            for (MessageFragment fragment : fragments) {
                if (!fragment.isReceivedAt(i)) {
                    continue;
                }
                String word = fragment.wordAt(i);
                if (chosen == null) {
                    chosen = word;
                } else if (!chosen.equals(word)) {
                    throw new InconsistentMessageException("The fragments disagree at position " + (i + 1)
                            + ": '" + chosen + "' and '" + word + "'.");
                }
            }
            if (chosen == null) {
                throw new InsufficientDataException("The word at position " + (i + 1) + " was not received by any device.");
            }
            words.add(chosen);
        }
        return String.join(" ", words);
    }
}
