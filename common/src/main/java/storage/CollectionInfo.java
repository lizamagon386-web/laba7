package storage;

import java.io.Serializable;
import java.time.LocalDate;

public record CollectionInfo(LocalDate creationDate, int elementsCount) implements Serializable {
    @Override
    public String toString() {
        return "CollectionInfo {" +
                "\n\tcreationDate: " + creationDate +
                "\n\tsize: " + elementsCount +
                "\n}";
    }
}
