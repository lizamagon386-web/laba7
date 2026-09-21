package domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Перечисление цветов.
 * Используется для задания цвета глаз, волос и других характеристик.
 */
public enum Color implements Serializable {
    /** Черный цвет */
    BLACK,
    /** Желтый цвет */
    YELLOW,
    /** Оранжевый цвет */
    ORANGE,
    /** Зеленый цвет */
    GREEN,
    /** Красный цвет */
    RED,
    /** Белый цвет */
    WHITE;

    /**
     * Карта соответствия числовых кодов значениям перечисления.
     * Используется для ввода цвета пользователем через числовой код.
     */
    public static final Map<Integer, Color> mapper = new HashMap<>() {{
        put(1, BLACK);
        put(2, YELLOW);
        put(3, ORANGE);
        put(4, GREEN);
        put(5, RED);
        put(6, WHITE);
    }};
}