# Лабораторная работа №7 (вариант 10710)

Клиент-серверное приложение для управления коллекцией объектов `Movie`.
Коллекция хранится в PostgreSQL, обмен между клиентом и сервером — по UDP.

## Структура проекта

| Модуль   | Назначение |
|----------|------------|
| `common` | общие классы: доменная модель (`Movie`, `Person`, `Location`, `Coordinates`), объекты запроса/ответа (`CommandRequest`, `CommandResponse`, `CommandType`), интерфейс `Storage` |
| `server` | приём и обработка запросов, коллекция в памяти, доступ к БД (`DbManager`, `server.dao.MovieDao`) |
| `client` | консольный клиент: авторизация, ввод объектов, отправка команд |
| `sql`    | `schema.sql` — таблицы, последовательности и ограничения |

## 1. Подготовка базы данных

```bash
psql -h pg -U <ваш_логин> -d studs -f sql/schema.sql
```

Скрипт создаёт:

* последовательности `user_id_seq` и `movie_id_seq` — id генерируется средствами БД (п.2 задания);
* таблицу `users` (логин, SHA-1 хэш пароля, соль);
* таблицу `movies` с внешним ключом `creator_id -> users.id` (п.8 задания).

## 2. Настройка подключения

Параметры берутся в порядке приоритета:

1. переменные окружения `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`;
2. системные свойства `-Ddb.host=... -Ddb.user=...`;
3. файл `server/src/main/resources/db.properties`;
4. значения по умолчанию (`pg`, `5432`, `studs`).

По умолчанию в `db.properties` прописаны хост `pg` и база `studs` — как требует задание.
Если сервер запускается не на кафедральном сервере, а локально через туннель:

```bash
ssh -L 5432:pg:5432 <логин>@helios.se.ifmo.ru
```

то нужно поставить `db.host=localhost`.

Чтобы не хранить пароль в репозитории, можно оставить `db.properties` пустым и передать данные через окружение:

```bash
export DB_USER=s505006
export DB_PASSWORD='...'
```

## 3. Сборка и запуск

```bash
mvn clean package
```

Собираются два самодостаточных jar-а (драйвер PostgreSQL и модуль `common` уже внутри):

```bash
java -jar server/target/server.jar              # сервер, порт 8080
java -jar client/target/client.jar              # клиент, по умолчанию localhost:8080
java -jar client/target/client.jar helios.se.ifmo.ru 8080   # хост и порт можно задать аргументами
```

## 4. Работа с приложением

При старте клиента доступно меню:

```
1 - войти
2 - зарегистрироваться
3 - выход
```

После входа логин и пароль отправляются вместе с каждым запросом (п.10 задания);
без успешной авторизации сервер отклоняет любые команды (п.7).

Команды: `help`, `info`, `show`, `add`, `update <id>`, `remove_by_id <id>`, `clear`,
`head`, `remove_head`, `remove_at <index>`, `add_if_min`, `sum_of_oscars_count`,
`count_less_than_oscars_count <n>`, `print_field_ascending_oscars_count`, `exit`.

Просматривать коллекцию могут все, изменять и удалять — только владелец объекта (п.9 задания).

## 5. Как реализованы требования задания

| Пункт | Где реализовано |
|-------|-----------------|
| 1. Хранение в PostgreSQL, без файла | `server.dao.MovieDao`, `server.DbManager`; классы XML-хранения (`MovieCollectionWrapper`, `LocalDateAdapter`, `datata.xml`) и зависимости JAXB удалены |
| 2. id через sequence | `movie_id_seq`, `DEFAULT nextval(...)` + `INSERT ... RETURNING id` |
| 3. Память обновляется только после успеха в БД | `CommandProcessor.commandAdd` и остальные команды изменения |
| 4. Чтение — из коллекции в памяти | `MovieCollection`, БД читается один раз при старте (`ServerRunner`) |
| 5. Регистрация и авторизация | `DbManager.register/authenticate`, меню `client.cmd.CommandLine.authorize()` |
| 6. SHA-1 | `DbManager.hashPassword` (SHA-1 от пароля с солью) |
| 7. Запрет команд без авторизации | проверка в начале `CommandProcessor.process` |
| 8. Сохранение владельца | колонка `movies.creator_id`, поле `Movie.creatorId` |
| 9. Смотреть — все, менять — владелец | проверки `getCreatorId() != userId` |
| 10. Логин/пароль с каждым запросом | поля `login`/`password` в `CommandRequest` |
| Многопоточность 1–3 | `ServerRunner`: поток чтения, поток на обработку, поток на отправку |
| Многопоточность 4 | `ReentrantLock` в `MovieCollection` и `CommandProcessor` |
