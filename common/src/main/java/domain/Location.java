package domain;

import java.io.Serializable;

/**
 * Класс, представляющий место рождения режиссёра.
 *
 * @param x    не может быть null
 * @param y    координата y
 * @param z    координата z
 * @param name не может быть null, длина не более 612 символов
 */
public class Location implements Comparable<Location>, Serializable {

    private Long x;

    private double y;

    private int z;

    private String name;

    /** Пустой конструктор для десериализации */
    public Location() {}

    public Location(Long x, double y, int z, String name) {
        assert x != null;
        assert name != null && name.length() < 612;
        this.x = x;
        this.y = y;
        this.z = z;
        this.name = name;
    }

    public Long x() { return x; }
    public double y() { return y; }
    public int z() { return z; }
    public String name() { return name; }

    @Override
    public String toString() {
        return "Location{x=" + x + ", y=" + y + ", z=" + z + ", name=" + name + "}";
    }

    @Override
    public int compareTo(Location o) {
        int xCompare = x.compareTo(o.x);
        if (xCompare != 0) return xCompare;
        int yCompare = Double.compare(y, o.y);
        if (yCompare != 0) return yCompare;
        int zCompare = Integer.compare(z, o.z);
        if (zCompare != 0) return zCompare;
        return name.compareTo(o.name);
    }
}