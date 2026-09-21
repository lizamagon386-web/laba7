package transport;

import domain.Movie;
import storage.CollectionInfo;

import java.io.Serializable;
import java.util.List;

public record CommandResponse(String result, String error, CollectionInfo info, List<Movie> movies)
        implements Serializable {
    public static CommandResponse ok(String message) {
        return new CommandResponse(message, null, null, null);
    }

    public static CommandResponse error(String message) {
        return new CommandResponse(null, message, null, null);
    }
}
