package domain;

import java.io.Serializable;

/**
 * Класс, представляющий координаты фильма.
 *
 * @param x значение поля должно быть больше -93
 * @param y координата y
 */
public class Coordinates implements Comparable<Coordinates>, Serializable {

    private int x;

    private double y;

    /** Пустой конструктор для десериализации */
    public Coordinates() {}

    public Coordinates(int x, double y) {
        assert x > -93;
        this.x = x;
        this.y = y;
    }

    public int x() { return x; }
    public double y() { return y; }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public int compareTo(Coordinates o) {
        if (x == o.x) return Double.compare(y, o.y);
        return Integer.compare(x, o.x);
    }
}