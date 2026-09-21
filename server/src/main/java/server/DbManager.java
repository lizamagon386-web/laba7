package server;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.HexFormat;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Управление подключением к PostgreSQL, регистрация и аутентификация пользователей.
 *
 * Пароли хранятся в виде SHA-1 хэша от (пароль + соль); соль генерируется
 * случайно при регистрации и хранится рядом с хэшем (требование п.6 задания).
 *
 * Параметры подключения берутся в порядке приоритета:
 * переменные окружения -> системные свойства (-D...) -> db.properties -> значения по умолчанию.
 * Это позволяет не держать пароль от БД в исходном коде.
 */
public class DbManager {
    public static final Logger logger = Logger.getLogger(DbManager.class.getName());

    private static final String DEFAULT_HOST = "pg";
    private static final String DEFAULT_PORT = "5432";
    private static final String DEFAULT_DB = "studs";

    private final String url;
    private final String user;
    private final String pass;

    public DbManager() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            logger.severe("Драйвер PostgreSQL не найден!");
        }

        Properties file = loadPropertiesFile();

        String host = resolve(file, "DB_HOST", "db.host", DEFAULT_HOST);
        String port = resolve(file, "DB_PORT", "db.port", DEFAULT_PORT);
        String dbName = resolve(file, "DB_NAME", "db.name", DEFAULT_DB);

        this.url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        this.user = resolve(file, "DB_USER", "db.user", null);
        this.pass = resolve(file, "DB_PASSWORD", "db.password", null);

        logger.info("Подключение к БД: " + url + " (пользователь: " + user + ")");
    }

    private static Properties loadPropertiesFile() {
        Properties props = new Properties();
        try (InputStream in = DbManager.class.getResourceAsStream("/db.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            logger.warning("Не удалось прочитать db.properties: " + e.getMessage());
        }
        return props;
    }

    /** Переменная окружения -> системное свойство -> db.properties -> значение по умолчанию. */
    private static String resolve(Properties file, String envName, String propName, String fallback) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()){
            return env;
        }

        String sys = System.getProperty(propName);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }

        String fromFile = file.getProperty(propName);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile;
        }

        return fallback;
    }

    public Connection getConnection() throws SQLException {
        if (user == null || pass == null) {
            throw new IllegalStateException(
                    "Не заданы логин/пароль к БД: укажите переменные окружения DB_USER и DB_PASSWORD "
                            + "либо заполните server/src/main/resources/db.properties");
        }
        return DriverManager.getConnection(url, user, pass);
    }

    /** Проверяет, что соединение с БД действительно устанавливается. Вызывается при старте сервера. */
    public void checkConnection() throws SQLException {
        try (Connection conn = getConnection()) {
            if (!conn.isValid(5)) throw new SQLException("Соединение с БД недействительно");
        }
    }

    /**
     * Хэширование пароля алгоритмом SHA-1 (требование п.6 задания).
     *
     * @param password пароль в открытом виде
     * @param salt     соль, добавляемая к паролю перед хэшированием
     * @return 40-символьный hex-хэш
     */
    public String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest((password + salt).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Алгоритм SHA-1 недоступен", e);
        }
    }

    private static String generateSalt() {
        byte[] salt = new byte[8];
        new SecureRandom().nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * Регистрирует нового пользователя.
     *
     * @return id нового пользователя, либо -1, если логин уже занят
     *         или произошла ошибка БД.
     */
    public int register(String login, String password) {
        if (login == null || login.isBlank() || password == null || password.isEmpty()) {
            return -1;
        }
        if (login.length() > 64) {
            logger.warning("Слишком длинный логин при регистрации");
            return -1;
        }

        String salt = generateSalt();
        String hashed = hashPassword(password, salt);

        try (Connection conn = getConnection();
             PreparedStatement insert = conn.prepareStatement(
                     "INSERT INTO users (name, password_digest, salt) VALUES (?, ?, ?) RETURNING id")) {

            insert.setString(1, login);
            insert.setString(2, hashed);
            insert.setString(3, salt);

            try (ResultSet rs = insert.executeQuery()) {
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    logger.info("Пользователь " + login + " зарегистрирован, id=" + newId);
                    return newId;
                }
            }
            return -1;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                logger.warning("Попытка регистрации с уже занятым логином: " + login);
            } else {
                logger.severe("Ошибка БД при регистрации: " + e.getMessage());
            }
            return -1;
        }
    }

    /**
     * Проверяет логин и пароль существующего пользователя.
     * Пользователя НЕ создаёт: если логина нет в БД, возвращает -1.
     *
     * @return id пользователя при успехе, либо -1 при ошибке аутентификации.
     */
    public int authenticate(String login, String password) {
        if (login == null || password == null) return -1;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, password_digest, salt FROM users WHERE name = ?")) {

            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    logger.warning("Попытка входа под несуществующим логином: " + login);
                    return -1;
                }
                String expected = rs.getString("password_digest");
                String salt = rs.getString("salt");
                if (expected != null && expected.equals(hashPassword(password, salt))) {
                    return rs.getInt("id");
                }
                logger.warning("Неверный пароль для пользователя " + login);
                return -1;
            }
        } catch (SQLException e) {
            logger.severe("Ошибка БД при авторизации: " + e.getMessage());
            return -1;
        }
    }
}
