package transport;

import domain.Movie;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record CommandRequest(UUID requestId, CommandType type, List<CommandArgument> arguments, Movie movie, String login, String password)
        implements Serializable {
    public CommandRequest {
        if (requestId == null) {
            requestId = UUID.randomUUID();
        }
    }
}
