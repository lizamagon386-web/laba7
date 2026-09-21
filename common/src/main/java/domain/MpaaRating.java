package domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public enum MpaaRating implements Serializable {
    PG,
    PG_13,
    NC_17;

    public static final Map<Integer, MpaaRating> mapper = new HashMap<>() {{
        put(1, PG);
        put(2, PG_13);
        put(3, NC_17);
    }};
}
