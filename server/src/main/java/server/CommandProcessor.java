package server;

import server.dao.MovieDao;
import domain.Movie;
import server.storage.MovieCollection;
import transport.CommandRequest;
import transport.CommandResponse;
import transport.CommandType;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Обработка команд, пришедших от клиента.
 *
 * Порядок работы с данными (требования пп.1, 3, 4 задания):
 *  - команды чтения (info, show, head, sum_of_oscars_count и т.д.) работают
 *    только с коллекцией в оперативной памяти;
 *  - команды изменения сначала выполняют запрос к БД и только при его успехе
 *    обновляют коллекцию в памяти.
 *
 * Авторизация (пп.5, 7, 10): логин и пароль приходят с каждым запросом.
 * AUTHENTICATE и REGISTER обрабатываются до проверки прав — это и есть
 * сама авторизация/регистрация; все остальные команды выполняются только
 * после успешной проверки пары логин/пароль.
 *
 * Модифицировать объект может только его владелец (пп.8, 9): creator_id
 * объекта сравнивается с id текущего пользователя.
 */
public class CommandProcessor {
    private final MovieCollection collection;
    private final DbManager dbManager;
    private final MovieDao movieDao;

    /**
     * Блокировка составных операций "прочитать из коллекции — записать в БД —
     * обновить коллекцию". Сама коллекция потокобезопасна, но без этой
     * блокировки два параллельных запроса могли бы, например, оба пройти
     * проверку владения и дважды удалить один и тот же объект.
     */
    private final ReentrantLock writeLock = new ReentrantLock();

    public CommandProcessor(MovieCollection collection, DbManager dbManager, MovieDao movieDao) {
        this.collection = collection;
        this.dbManager = dbManager;
        this.movieDao = movieDao;
    }

    public CommandResponse process(CommandRequest request) {
        try {
            if (request == null || request.type() == null) {
                return CommandResponse.error("Неизвестная команда");
            }

            if (request.type() == CommandType.AUTHENTICATE) {
                return commandAuthenticate(request);
            }
            if (request.type() == CommandType.REGISTER) {
                return commandRegister(request);
            }

            int userId = dbManager.authenticate(request.login(), request.password());
            if (userId <= 0) {
                return CommandResponse.error("Ошибка авторизации: неверный логин или пароль.");
            }

            return switch (request.type()) {
                case HELP -> CommandResponse.ok("Список команд доступен на клиенте");
                case INFO -> new CommandResponse(null, null, collection.info(), null);
                case SHOW -> new CommandResponse(null, null, null, collection.show());

                case ADD -> commandAdd(request, userId);
                case UPDATE -> commandUpdate(request, userId);
                case REMOVE_BY_ID -> commandRemoveById(request, userId);
                case CLEAR -> commandClear(userId);

                case HEAD -> {
                    Movie head = collection.head();
                    yield head != null
                            ? new CommandResponse(null, null, null, List.of(head))
                            : CommandResponse.error("Коллекция пуста");
                }
                case REMOVE_HEAD -> commandRemoveHead(userId);
                case REMOVE_AT -> commandRemoveAt(request, userId);

                case ADD_IF_MIN -> commandAddIfMin(request, userId);

                case SUM_OF_OSCARS_COUNT -> CommandResponse.ok("Сумма оскаров: " + collection.sumOfOscarsCount());
                case COUNT_LESS_THAN_OSCARS_COUNT -> commandCountLessOscars(request);
                case PRINT_FIELD_ASCENDING_OSCARS_COUNT -> {
                    List<Integer> oscars = collection.printFieldAscendingOscarsCount();
                    yield CommandResponse.ok("Оскары по возрастанию: " + oscars);
                }
                default -> CommandResponse.error("Неподдерживаемая команда");
            };
        } catch (Exception e) {
            return CommandResponse.error("Ошибка на сервере: " + e.getMessage());
        }
    }

    private CommandResponse commandRegister(CommandRequest request) {
        int newId = dbManager.register(request.login(), request.password());
        if (newId <= 0) {
            return CommandResponse.error("Не удалось зарегистрироваться: логин уже занят или данные некорректны.");
        }
        return CommandResponse.ok("Регистрация успешна, ваш логин: " + request.login());
    }

    private CommandResponse commandAuthenticate(CommandRequest request) {
        int userId = dbManager.authenticate(request.login(), request.password());
        if (userId <= 0) {
            return CommandResponse.error("Неверный логин или пароль");
        }
        return CommandResponse.ok("Авторизация успешна");
    }

    /** Добавление: сначала INSERT в БД (id выдаёт sequence), при успехе — в коллекцию. */
    private CommandResponse commandAdd(CommandRequest request, int userId) {
        Movie movie = request.movie();
        if (movie == null) return CommandResponse.error("Не переданы данные фильма");
        try {
            int generatedId = movieDao.insert(movie, userId);
            if (generatedId == -1) return CommandResponse.error("Не удалось добавить фильм");

            movie.setId(generatedId);
            movie.setCreatorId(userId);
            collection.add(movie);
            return CommandResponse.ok("Фильм добавлен. ID: " + generatedId);
        } catch (SQLException e) {
            return CommandResponse.error("Ошибка БД: " + e.getMessage());
        }
    }

    private CommandResponse commandUpdate(CommandRequest request, int userId) {
        if (request.arguments().isEmpty()) return CommandResponse.error("Не указан ID");
        Movie updated = request.movie();
        if (updated == null) return CommandResponse.error("Не переданы данные фильма");

        writeLock.lock();
        try {
            int id = Integer.parseInt(request.arguments().get(0).value());
            Movie movieToUpdate = collection.findById(id);

            if (movieToUpdate == null) return CommandResponse.error("Фильм не найден");
            if (movieToUpdate.getCreatorId() != userId) return CommandResponse.error("Вы не владелец!");

            updated.setId(id);
            updated.setCreatorId(userId);

            movieDao.update(updated);
            collection.update(id, updated);
            return CommandResponse.ok("Объект обновлен");
        } catch (NumberFormatException e) {
            return CommandResponse.error("ID должен быть числом");
        } catch (SQLException e) {
            return CommandResponse.error("Ошибка БД при обновлении: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    private CommandResponse commandRemoveById(CommandRequest request, int userId) {
        if (request.arguments().isEmpty()) return CommandResponse.error("Не указан ID");
        writeLock.lock();
        try {
            int id = Integer.parseInt(request.arguments().get(0).value());
            return removeOwned(collection.findById(id), userId);
        } catch (NumberFormatException e) {
            return CommandResponse.error("ID должен быть числом");
        } catch (SQLException e) {
            return CommandResponse.error("Ошибка БД при удалении: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    /** Удаляет элемент по индексу в отсортированной коллекции, если он принадлежит пользователю. */
    private CommandResponse commandRemoveAt(CommandRequest request, int userId) {
        if (request.arguments().isEmpty()) return CommandResponse.error("Не указан индекс");
        writeLock.lock();
        try {
            int index = Integer.parseInt(request.arguments().get(0).value());
            List<Movie> sorted = collection.show();
            if (index < 0 || index >= sorted.size()) return CommandResponse.error("Некорректный индекс");

            return removeOwned(sorted.get(index), userId);
        } catch (NumberFormatException e) {
            return CommandResponse.error("Индекс должен быть числом");
        } catch (SQLException e) {
            return CommandResponse.error("Ошибка БД при удалении: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    private CommandResponse commandRemoveHead(int userId) {
        writeLock.lock();
        try {
            Movie head = collection.head();
            if (head == null) return CommandResponse.error("Коллекция пуста");
            if (head.getCreatorId() != userId) return CommandResponse.error("Вы не владелец первого элемента");

            movieDao.delete(head.getId());
            collection.removeHead();
            return CommandResponse.ok("Ваш головной элемент удален");
        } catch (SQLException e) {
            return CommandResponse.error("Ошибка БД при удалении: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    /** clear удаляет только объекты текущего пользователя (п.9 задания). */
    private CommandResponse commandClear(int userId) {
        writeLock.lock();
        try {
            List<Integer> idsToRemove = collection.show().stream()
                    .filter(m -> m.getCreatorId() == userId)
                    .map(Movie::getId)
                    .collect(Collectors.toList());

            int removed = movieDao.deleteAllByCreator(userId);
            idsToRemove.forEach(collection::removeById);
            return CommandResponse.ok("Ваши объекты удалены из коллекции (всего: " + removed + ")");
        } catch (SQLException e) {
            return CommandResponse.error("Ошибка БД при очистке: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    private CommandResponse commandAddIfMin(CommandRequest request, int userId) {
        Movie movie = request.movie();
        if (movie == null) return CommandResponse.error("Не переданы данные фильма");
        writeLock.lock();
        try {
            boolean isMin = collection.show().stream().min(Movie::compareTo)
                    .map(min -> movie.compareTo(min) < 0)
                    .orElse(true);
            if (!isMin) return CommandResponse.ok("Элемент не меньше минимального, добавление отменено");
            return commandAdd(request, userId);
        } finally {
            writeLock.unlock();
        }
    }

    private CommandResponse commandCountLessOscars(CommandRequest request) {
        if (request.arguments().isEmpty()) return CommandResponse.error("Не указано количество оскаров");
        try {
            int oscars = Integer.parseInt(request.arguments().get(0).value());
            return CommandResponse.ok("Найдено элементов: " + collection.countLessThanOscarsCount(oscars));
        } catch (NumberFormatException e) {
            return CommandResponse.error("Количество оскаров должно быть числом");
        }
    }

    /**
     * Общая часть remove_by_id / remove_at: проверка владения,
     * удаление из БД и только затем — из коллекции в памяти.
     */
    private CommandResponse removeOwned(Movie movie, int userId) throws SQLException {
        if (movie == null) return CommandResponse.error("Объект не найден");
        if (movie.getCreatorId() != userId) return CommandResponse.error("Вы не владелец!");

        if (movieDao.delete(movie.getId()) == 0) {
            return CommandResponse.error("Объект уже удалён из базы данных");
        }
        collection.removeById(movie.getId());
        return CommandResponse.ok("Объект удален");
    }
}
