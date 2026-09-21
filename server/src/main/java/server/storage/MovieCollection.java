package server.storage;

import domain.Movie;
import storage.CollectionInfo;
import storage.Storage;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Коллекция фильмов, живущая в оперативной памяти сервера.
 *
 * Начальное состояние загружается из PostgreSQL при старте сервера,
 * дальше все команды чтения обслуживаются отсюда, без обращения к БД
 * (пп.1 и 4 задания). Сохранение в файл убрано полностью.
 *
 * Доступ к коллекции синхронизирован через
 * {@link java.util.concurrent.locks.ReentrantLock} — сервер обрабатывает
 * каждый запрос в отдельном потоке.
 */
public class MovieCollection implements Storage {
    private final ReentrantLock lock = new ReentrantLock();
    private final Stack<Movie> stack;
    private final LocalDate creationDate;

    public MovieCollection() {
        this.stack = new Stack<>();
        this.creationDate = LocalDate.now();
    }

    @Override
    public CollectionInfo info() {
        lock.lock();
        try {
            return new CollectionInfo(creationDate, stack.size());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Movie> show() {
        lock.lock();
        try {
            return stack.stream()
                    .sorted(Comparator.comparing(Movie::getName))
                    .collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Находит элемент коллекции по id.
     *
     * @param id идентификатор фильма
     * @return найденный фильм либо null
     */
    public Movie findById(int id) {
        lock.lock();
        try {
            return stack.stream().filter(m -> m.getId() == id).findFirst().orElse(null);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void add(Movie m) {
        lock.lock();
        try {
            stack.push(m);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void update(int id, Movie m) {
        lock.lock();
        try {
            stack.stream().filter(movie -> movie.getId() == id).findFirst().ifPresent(movie -> {
                movie.setName(m.getName());
                movie.setCoordinates(m.getCoordinates());
                movie.setGenre(m.getGenre());
                movie.setScreenwriter(m.getScreenwriter());
                movie.setMpaaRating(m.getMpaaRating());
                movie.setOscarsCount(m.getOscarsCount());
            });
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeById(int id) {
        lock.lock();
        try {
            stack.removeIf(m -> m.getId() == id);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            stack.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override public Movie head() {
        lock.lock();
        try { return stack.isEmpty() ? null : stack.firstElement(); }
        finally { lock.unlock(); }
    }

    @Override public Movie removeHead() {
        lock.lock();
        try { return stack.isEmpty() ? null : stack.remove(0); }
        finally { lock.unlock(); }
    }

    @Override public void removeAt(int i) {
        lock.lock();
        try { if (i >= 0 && i < stack.size()) stack.remove(i); }
        finally { lock.unlock(); }
    }

    @Override public Long sumOfOscarsCount() {
        lock.lock();
        try { return stack.stream().mapToLong(m -> m.getOscarsCount() == null ? 0 : m.getOscarsCount()).sum(); }
        finally { lock.unlock(); }
    }

    @Override public int countLessThanOscarsCount(int oscars) {
        lock.lock();
        try { return (int) stack.stream().filter(m -> m.getOscarsCount() != null && m.getOscarsCount() < oscars).count(); }
        finally { lock.unlock(); }
    }

    @Override public void addIfMin(Movie m) {
        lock.lock();
        try { if (stack.isEmpty() || m.compareTo(Collections.min(stack)) < 0) stack.add(m); }
        finally { lock.unlock(); }
    }

    @Override public void sort() {
        lock.lock();
        try { Collections.sort(stack); }
        finally { lock.unlock(); }
    }

    @Override public List<Integer> printFieldAscendingOscarsCount() {
        lock.lock();
        try { return stack.stream().map(Movie::getOscarsCount).filter(Objects::nonNull).sorted().collect(Collectors.toList()); }
        finally { lock.unlock(); }
    }
}
