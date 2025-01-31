package ChessEngine;

import java.time.Duration;
import java.time.Instant;

public class SearchManager {
    private final Instant searchStartTime;
    private final long timeLimit;

    public SearchManager(long timeLimitMillis) {
        this.searchStartTime = Instant.now();
        this.timeLimit = timeLimitMillis;
    }

    public boolean shouldCancel() {
        long elapsed = Duration.between(searchStartTime, Instant.now()).toMillis();
        return elapsed >= timeLimit;
    }
}
