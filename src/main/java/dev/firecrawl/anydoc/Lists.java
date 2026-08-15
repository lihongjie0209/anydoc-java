package dev.firecrawl.anydoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Java 8 stand-in for {@code List.copyOf}. */
final class Lists {

    private Lists() {}

    static <T> List<T> copyOf(List<T> items) {
        return Collections.unmodifiableList(new ArrayList<T>(Objects.requireNonNull(items)));
    }
}
