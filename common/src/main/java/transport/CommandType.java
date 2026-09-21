package transport;

import java.io.Serializable;

public enum CommandType implements Serializable {
    HELP("help"),
    INFO("info"),
    SHOW("show"),
    ADD("add"),
    UPDATE("update"),
    REMOVE_BY_ID("remove_by_id"),
    CLEAR("clear"),
    HEAD("head"),
    REMOVE_HEAD("remove_head"),
    REMOVE_AT("remove_at"),
    ADD_IF_MIN("add_if_min"),
    SUM_OF_OSCARS_COUNT("sum_of_oscars_count"),
    COUNT_LESS_THAN_OSCARS_COUNT("count_less_than_oscars_count"),
    PRINT_FIELD_ASCENDING_OSCARS_COUNT("print_field_ascending_oscars_count"),
    AUTHENTICATE("authenticate"),
    REGISTER("register");

    private final String wireName;

    CommandType(String wireName) {
        this.wireName = wireName;
    }

    public static CommandType fromWireName(String s) {
        for (CommandType t : values()) {
            if (t.wireName.equals(s)) return t;
        }
        return null;
    }
}
