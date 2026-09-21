package client.cmd;

import java.util.List;
import java.util.function.Consumer;

public record Command(String name, Consumer<List<String>> exec, String description) {
}
