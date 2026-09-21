package domain;

import java.io.Serializable;

/**
 * Класс, представляющий человека (режиссёра).
 *
 * @param name      не может быть null, строка не может быть пустой
 * @param weight    значение поля должно быть больше 0
 * @param eyeColor  не может быть null
 * @param hairColor может быть null
 * @param location  может быть null
 */
public class Person implements Comparable<Person>, Serializable {

    private String name;

    private int weight;

    private Color eyeColor;

    private Color hairColor;

    private Location location;

    /** Пустой конструктор для десериализации */
    public Person() {}

    public Person(String name, int weight, Color eyeColor, Color hairColor, Location location) {
        assert name != null;
        assert weight > 0;
        assert eyeColor != null;
        this.name = name;
        this.weight = weight;
        this.eyeColor = eyeColor;
        this.hairColor = hairColor;
        this.location = location;
    }

    public String name() { return name; }
    public int weight() { return weight; }
    public Color eyeColor() { return eyeColor; }
    public Color hairColor() { return hairColor; }
    public Location location() { return location; }

    @Override
    public String toString() {
        return "Person{name=" + name + ", weight=" + weight +
                ", eyeColor=" + eyeColor + ", hairColor=" + hairColor +
                ", location=" + location + "}";
    }

    /**
     * Сравнивает текущего человека с другим последовательно по имени,
     * весу, цвету глаз, волос и локации.
     *
     * @param o объект Person для сравнения
     * @return отрицательное число, ноль или положительное число
     */
    @Override
    public int compareTo(Person o) {
        int nameCompare = name.compareTo(o.name);
        if (nameCompare != 0) return nameCompare;
        int weightCompare = Integer.compare(weight, o.weight);
        if (weightCompare != 0) return weightCompare;
        int eyeCompare = eyeColor.compareTo(o.eyeColor);
        if (eyeCompare != 0) return eyeCompare;
        int hairCompare = hairColor.compareTo(o.hairColor);
        if (hairCompare != 0) return hairCompare;
        return location.compareTo(o.location);
    }
}