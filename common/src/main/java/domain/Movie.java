package domain;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Класс, представляющий фильм.
 * Содержит информацию о фильме: дату создания, количество оскаров, жанр, рейтинг MPAA и сценариста.
 *
 * Владелец объекта (creatorId) хранится как числовой id пользователя
 * из таблицы users — на него ссылается внешний ключ movies.creator_id.
 */

public class Movie implements Comparable<Movie>, Serializable {


    private Integer id;


    private LocalDate creationDate;


    private String name;


    private Coordinates coordinates;


    private Integer oscarsCount;


    private MovieGenre genre;


    private MpaaRating mpaaRating;


    private Person screenwriter;

    /** id пользователя (из таблицы users), создавшего этот фильм. 0, если ещё не присвоен. */
    private int creatorId;

    /** Пустой конструктор для десериализации */
    public Movie() {
        this.creationDate = LocalDate.now();
        this.id = 0;
    }

    /**
     * Конструктор для создания нового фильма с автоматической генерацией id и даты создания.
     * creatorId присваивается позже, на сервере, после определения текущего пользователя.
     *
     * @param name         название фильма
     * @param coordinates  координаты фильма
     * @param oscarsCount  количество оскаров
     * @param genre        жанр фильма
     * @param mpaaRating   рейтинг MPAA
     * @param screenwriter сценарист фильма
     * @throws IllegalArgumentException если name, coordinates, genre или mpaaRating равны null
     */
    public Movie(String name, Coordinates coordinates, Integer oscarsCount, MovieGenre genre, MpaaRating mpaaRating, Person screenwriter) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Имя не может быть пустым");
        if (coordinates == null) throw new IllegalArgumentException("Координаты не могут быть null");
        if (genre == null) throw new IllegalArgumentException("Жанр не может быть null");
        if (mpaaRating == null) throw new IllegalArgumentException("Рейтинг не может быть null");
        this.id = 0;
        this.creationDate = LocalDate.now();
        this.name = name;
        this.coordinates = coordinates;
        this.oscarsCount = oscarsCount;
        this.genre = genre;
        this.mpaaRating = mpaaRating;
        this.screenwriter = screenwriter;
    }

    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }

    /**
     * Конструктор для загрузки существующего фильма из коллекции (например, из БД).
     *
     * @param id           идентификатор фильма
     * @param name         название фильма
     * @param coordinates  координаты фильма
     * @param creationDate дата создания фильма
     * @param oscarsCount  количество оскаров
     * @param genre        жанр фильма
     * @param mpaaRating   рейтинг MPAA
     * @param screenwriter сценарист фильма
     * @param creatorId    id пользователя-создателя
     */
    public Movie(Integer id, String name, Coordinates coordinates, LocalDate creationDate, Integer oscarsCount, MovieGenre genre, MpaaRating mpaaRating, Person screenwriter, int creatorId) {
        this.id = id;
        this.creationDate = creationDate;
        this.name = name;
        this.coordinates = coordinates;
        this.oscarsCount = oscarsCount;
        this.genre = genre;
        this.mpaaRating = mpaaRating;
        this.screenwriter = screenwriter;
        this.creatorId = creatorId;
    }

    /**
     * Возвращает строковое представление фильма.
     *
     * @return строка с информацией о фильме
     */
    @Override
    public String toString() {
        return "Movie {" +
                "\n\tid: " + id +
                "\n\tname: " + name +
                "\n\tcoordinates: " + coordinates +
                "\n\tcreationDate: " + creationDate +
                "\n\toscarsCount: " + oscarsCount +
                "\n\tgenre: " + genre +
                "\n\tmpaaRating: " + mpaaRating +
                "\n\tscreenwriter: " + screenwriter +
                "\n\tcreatorId: " + creatorId +
                "\n}";
    }

    /** @return идентификатор фильма */
    public Integer getId() { return id; }
    /** @return название фильма */
    public String getName() { return name; }
    /** @return координаты фильма */
    public Coordinates getCoordinates() { return coordinates; }
    /** @return дата создания фильма */
    public LocalDate getCreationDate() { return creationDate; }
    /** @return количество оскаров */
    public Integer getOscarsCount() { return oscarsCount; }
    /** @return жанр фильма */
    public MovieGenre getGenre() { return genre; }
    /** @return рейтинг MPAA */
    public MpaaRating getMpaaRating() { return mpaaRating; }
    /** @return сценарист фильма */
    public Person getScreenwriter() { return screenwriter; }

    /** @param id новый идентификатор */
    public void setId(Integer id) { this.id = id; }
    /** @param name новое название */
    public void setName(String name) { this.name = name; }
    /** @param coordinates новые координаты */
    public void setCoordinates(Coordinates coordinates) { this.coordinates = coordinates; }
    /** @param oscarsCount новое количество оскаров */
    public void setOscarsCount(Integer oscarsCount) { this.oscarsCount = oscarsCount; }
    /** @param genre новый жанр */
    public void setGenre(MovieGenre genre) { this.genre = genre; }
    /** @param mpaaRating новый рейтинг */
    public void setMpaaRating(MpaaRating mpaaRating) { this.mpaaRating = mpaaRating; }
    /** @param screenwriter новый сценарист */
    public void setScreenwriter(Person screenwriter) { this.screenwriter = screenwriter; }

    /**
     * Сравнивает текущий фильм с другим сначала по названию, затем по id.
     *
     * @param o фильм для сравнения
     * @return результат сравнения
     */
    @Override
    public int compareTo(Movie o) {
        if (this.name == null || o.name == null) return 0;
        int nameCompare = name.compareTo(o.name);
        if (nameCompare != 0) return nameCompare;
        return Integer.compare(this.id, o.id);
    }
}
