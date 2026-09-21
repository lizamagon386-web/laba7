package domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public enum MovieGenre implements Serializable {
    WESTERN,
    ADVENTURE,
    HORROR,
    SCIENCE_FICTION;

    public static final Map<Integer, MovieGenre> mapper = new HashMap<>() {{
        put(1, WESTERN);
        put(2, ADVENTURE);
        put(3, HORROR);
        put(4, SCIENCE_FICTION);
    }};
}
