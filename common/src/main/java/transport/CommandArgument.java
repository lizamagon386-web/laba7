package transport;

import java.io.Serializable;

public record CommandArgument(String value) implements Serializable {
}
