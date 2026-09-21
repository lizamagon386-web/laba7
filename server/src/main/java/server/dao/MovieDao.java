package server.dao;

import domain.*;
import server.DbManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Доступ к таблице movies.
 *
 * Все изменения коллекции идут сначала сюда и только при успехе
 * попадают в коллекцию в памяти (п.3 задания). id новой записи
 * выдаёт последовательность movie_id_seq на стороне БД (п.2 задания).
 */
public class MovieDao {
    private final DbManager dbManager;

    public MovieDao(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Загрузка всей коллекции из БД.
     * Вызывается один раз при старте сервера — дальше коллекция
     * живёт в памяти (требование задания).
     */
    public List<Movie> findAll() throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String sql = """
            SELECT id, name, coordinate_x, coordinate_y, creation_date, oscars_count,
                   genre, mpaa_rating, screenwriter_name, screenwriter_weight,
                   screenwriter_eye_color, screenwriter_hair_color,
                   location_x, location_y, location_z, location_name, creator_id
            FROM movies
            """;

        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                movies.add(mapResultSetToMovie(rs));
            }
        }
        return movies;
    }

    /**
     * Вставка нового фильма. id назначается sequence'ом БД
     * (DEFAULT nextval('movie_id_seq') в определении таблицы) —
     * здесь только считываем сгенерированный id обратно через RETURNING id.
     *
     * @param m         фильм для вставки (без id)
     * @param creatorId числовой id пользователя-создателя
     * @return сгенерированный id, либо -1 при ошибке
     */
    public int insert(Movie m, int creatorId) throws SQLException {
        String sql = """
            INSERT INTO movies (name, coordinate_x, coordinate_y, creation_date, oscars_count,
                                 genre, mpaa_rating, screenwriter_name, screenwriter_weight,
                                 screenwriter_eye_color, screenwriter_hair_color,
                                 location_x, location_y, location_z, location_name, creator_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, m.getName());
            ps.setInt(2, m.getCoordinates().x());
            ps.setDouble(3, m.getCoordinates().y());
            ps.setDate(4, Date.valueOf(m.getCreationDate()));
            if (m.getOscarsCount() != null) ps.setInt(5, m.getOscarsCount());
            else ps.setNull(5, Types.INTEGER);
            ps.setString(6, m.getGenre().name());
            ps.setString(7, m.getMpaaRating().name());

            bindScreenwriter(ps, 8, m.getScreenwriter());
            ps.setInt(16, creatorId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    /**
     * Обновляет существующий фильм. Проверка владения выполняется
     * на уровне CommandProcessor, не здесь.
     */
    public void update(Movie m) throws SQLException {
        String sql = """
            UPDATE movies
            SET name = ?, coordinate_x = ?, coordinate_y = ?, oscars_count = ?,
                genre = ?, mpaa_rating = ?, screenwriter_name = ?, screenwriter_weight = ?,
                screenwriter_eye_color = ?, screenwriter_hair_color = ?,
                location_x = ?, location_y = ?, location_z = ?, location_name = ?
            WHERE id = ?
            """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, m.getName());
            ps.setInt(2, m.getCoordinates().x());
            ps.setDouble(3, m.getCoordinates().y());
            if (m.getOscarsCount() != null) ps.setInt(4, m.getOscarsCount());
            else ps.setNull(4, Types.INTEGER);
            ps.setString(5, m.getGenre().name());
            ps.setString(6, m.getMpaaRating().name());

            bindScreenwriter(ps, 7, m.getScreenwriter());
            ps.setInt(15, m.getId());

            ps.executeUpdate();
        }
    }

    /**
     * Удаление фильма по id. Проверка владения выполняется заранее,
     * на уровне CommandProcessor (там же, где есть полный объект Movie
     * с creatorId, загруженный из памяти).
     *
     * @return количество удалённых строк (0 или 1)
     */
    public int delete(int id) throws SQLException {
        String sql = "DELETE FROM movies WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    /**
     * Удаляет все фильмы, созданные указанным пользователем (команда clear).
     * Выполняется одним запросом, поэтому либо удаляются все объекты
     * пользователя, либо, при ошибке, ни одного.
     *
     * @param creatorId id пользователя-владельца
     * @return количество удалённых строк
     */
    public int deleteAllByCreator(int creatorId) throws SQLException {
        String sql = "DELETE FROM movies WHERE creator_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, creatorId);
            return ps.executeUpdate();
        }
    }

    /** Привязывает поля сценариста (Person) к PreparedStatement, начиная с индекса startIndex (8 параметров). */
    private void bindScreenwriter(PreparedStatement ps, int startIndex, Person p) throws SQLException {
        if (p != null) {
            ps.setString(startIndex, p.name());
            ps.setInt(startIndex + 1, p.weight());
            ps.setString(startIndex + 2, p.eyeColor().name());
            ps.setString(startIndex + 3, p.hairColor() == null ? null : p.hairColor().name());
            Location loc = p.location();
            if (loc != null) {
                ps.setLong(startIndex + 4, loc.x());
                ps.setDouble(startIndex + 5, loc.y());
                ps.setInt(startIndex + 6, loc.z());
                ps.setString(startIndex + 7, loc.name());
            } else {
                ps.setNull(startIndex + 4, Types.BIGINT);
                ps.setNull(startIndex + 5, Types.DOUBLE);
                ps.setNull(startIndex + 6, Types.INTEGER);
                ps.setNull(startIndex + 7, Types.VARCHAR);
            }
        } else {
            ps.setNull(startIndex, Types.VARCHAR);
            ps.setNull(startIndex + 1, Types.INTEGER);
            ps.setNull(startIndex + 2, Types.VARCHAR);
            ps.setNull(startIndex + 3, Types.VARCHAR);
            ps.setNull(startIndex + 4, Types.BIGINT);
            ps.setNull(startIndex + 5, Types.DOUBLE);
            ps.setNull(startIndex + 6, Types.INTEGER);
            ps.setNull(startIndex + 7, Types.VARCHAR);
        }
    }

    private Movie mapResultSetToMovie(ResultSet rs) throws SQLException {
        Coordinates coordinates = new Coordinates(rs.getInt("coordinate_x"), rs.getDouble("coordinate_y"));

        String screenwriterName = rs.getString("screenwriter_name");
        Person screenwriter = null;
        if (screenwriterName != null) {
            Object locXObj = rs.getObject("location_x");
            Long locX = locXObj != null ? rs.getLong("location_x") : null;
            Location location = null;
            if (locX != null) {
                location = new Location(locX, rs.getDouble("location_y"), rs.getInt("location_z"), rs.getString("location_name"));
            }
            String hairColorStr = rs.getString("screenwriter_hair_color");
            screenwriter = new Person(
                    screenwriterName,
                    rs.getInt("screenwriter_weight"),
                    Color.valueOf(rs.getString("screenwriter_eye_color")),
                    hairColorStr == null ? null : Color.valueOf(hairColorStr),
                    location);
        }

        Integer oscarsCount = rs.getObject("oscars_count") != null ? rs.getInt("oscars_count") : null;

        return new Movie(
                rs.getInt("id"),
                rs.getString("name"),
                coordinates,
                rs.getDate("creation_date").toLocalDate(),
                oscarsCount,
                MovieGenre.valueOf(rs.getString("genre")),
                MpaaRating.valueOf(rs.getString("mpaa_rating")),
                screenwriter,
                rs.getInt("creator_id"));
    }
}
