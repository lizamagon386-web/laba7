package storage;

import domain.Movie;

import java.util.List;

/**
 * Контракт хранилища коллекции.
 *
 * Методы save()/load() убраны: коллекция больше не сохраняется в файл,
 * её постоянное хранилище — реляционная СУБД (п.1 задания).
 */
public interface Storage {
    CollectionInfo info();
    List<Movie> show();
    void add(Movie m);
    void update(int id, Movie m);
    void removeById(int id);
    void clear();
    Movie head();
    Movie removeHead();
    void removeAt(int i);
    void addIfMin(Movie m);
    void sort();
    Long sumOfOscarsCount();
    int countLessThanOscarsCount(int oscars);
    List<Integer> printFieldAscendingOscarsCount();
}
