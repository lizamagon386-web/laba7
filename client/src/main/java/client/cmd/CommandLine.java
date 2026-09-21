package client.cmd;

import client.UDPClient;
import domain.*;
import transport.CommandArgument;
import transport.CommandRequest;
import transport.CommandResponse;
import transport.CommandType;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Работа с консолью.
 * Реализует чтение команд, парсинг аргументов и вызов необходимых команд.
 */
public class CommandLine implements Runnable {
    private String login;
    private String password;

    private final List<Command> commands = new ArrayList<>() {{
        add(new Command("help", x -> help(), "вывести справку"));
        add(new Command("info", x -> sendCommand("info", x), "информация о коллекции"));
        add(new Command("show", x -> sendCommand("show", x), "вывести все элементы"));
        add(new Command("add", x -> sendCommandWithMovie("add", x), "добавить элемент"));
        add(new Command("update", x -> sendCommandWithMovie("update", x), "обновить по id"));
        add(new Command("remove_by_id", x -> sendCommand("remove_by_id", x), "удалить по id"));
        add(new Command("clear", x -> sendCommand("clear", x), "очистить"));
        add(new Command("exit", x -> { throw new CommandLineExitException(); }, "выход"));
        add(new Command("head", x -> sendCommand("head", x), "первый элемент"));
        add(new Command("remove_head", x -> sendCommand("remove_head", x), "удалить первый"));
        add(new Command("remove_at", x -> sendCommand("remove_at", x), "удалить по индексу"));
        add(new Command("add_if_min", x -> sendCommandWithMovie("add_if_min", x), "добавить если минимум"));
        add(new Command("sum_of_oscars_count", x -> sendCommand("sum_of_oscars_count", x), "сумма оскаров"));
        add(new Command("count_less_than_oscars_count", x -> sendCommand("count_less_than_oscars_count", x), "фильтровать по оскарам"));
        add(new Command("print_field_ascending_oscars_count", x -> sendCommand("print_field_ascending_oscars_count", x), "оскары по возрастанию"));
    }};

    private final Map<String, Command> commandMap = new HashMap<>() {{
        for (Command c : commands) put(c.name(), c);
    }};

    private final UDPClient client;
    private static final Scanner scanner = new Scanner(System.in);

    public CommandLine(UDPClient client) {
        this.client = client;
    }

    private void help() {
        for (Command c : commands) {
            System.out.printf("- %s: %s\n", c.name(), c.description());
        }
    }

    private List<CommandArgument> toArguments(List<String> args) {
        return args.stream().map(CommandArgument::new).collect(Collectors.toList());
    }

    private void sendCommand(String commandName, List<String> args) {
        try {
            CommandRequest request = new CommandRequest(
                    null,
                    CommandType.fromWireName(commandName),
                    toArguments(args),
                    null,
                    this.login,
                    this.password
            );
            printResponse(client.sendRequest(request));
        } catch (IOException e) {
            System.out.println("Сервер недоступен: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void sendCommandWithMovie(String commandName, List<String> args) {
        Movie movie = inputMovie();
        try {
            CommandRequest request = new CommandRequest(
                    null,
                    CommandType.fromWireName(commandName),
                    toArguments(args),
                    movie,
                    this.login,
                    this.password
            );
            printResponse(client.sendRequest(request));
        } catch (IOException e) {
            System.out.println("Сервер недоступен: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void printResponse(CommandResponse response) {
        if (response.error() != null) {
            System.out.println("Ошибка: " + response.error());
            return;
        }
        if (response.result() != null) System.out.println(response.result());
        if (response.info() != null) System.out.println(response.info());
        if (response.movies() != null) {
            if (response.movies().isEmpty()) {
                System.out.println("Коллекция пустая");
            } else {
                response.movies().forEach(System.out::println);
            }
        }
    }

    private static Movie inputMovie() {
        System.out.print("Введите имя фильма: ");
        String name = scanner.nextLine().replace("\n", "");
        return new Movie(
                name,
                inputCoordinates(),
                inputOscarCount(),
                inputMovieGenre(),
                inputMpaaRating(),
                inputScreenwriter());
    }

    private static Color inputScreenwriterColor(String about) {
        while (true) {
            System.out.println("Введите цвет " + about + " режиссера: ");
            printEnumDesc(Color.mapper);
            System.out.print("Цвет " + about + ": ");
            String input = scanner.nextLine().trim();
            if (isInteger(input)) {
                int i = Integer.parseInt(input);
                if (Color.mapper.containsKey(i)) return Color.mapper.get(i);
            }
            System.out.println("Ошибка, выберите число из списка");
        }
    }

    private static Color inputScreenwriterHairColor() {
        return inputScreenwriterColor("волос");
    }

    private static Color inputScreenwriterEyeColor() {
        return inputScreenwriterColor("глаз");
    }

    private static int inputScreenwriterWeight() {
        while (true) {
            System.out.print("Введите вес режиссера: ");
            String input = scanner.nextLine().trim();
            if (isInteger(input)) {
                int weight = Integer.parseInt(input);
                if (weight > 0) return weight;
                System.out.println("Ошибка, вес должен быть больше нуля");
            } else {
                System.out.println("Ошибка, введите целое число");
            }
        }
    }

    private static Person inputScreenwriter() {
        while (true) {
            System.out.print("Хотите указать режиссера? (y/n): ");
            String ans = scanner.nextLine().trim().toLowerCase();
            if (ans.equals("y")) {
                return new Person(
                        inputScreenwriterName(),
                        inputScreenwriterWeight(),
                        inputScreenwriterEyeColor(),
                        inputScreenwriterHairColor(),
                        inputLocation());
            } else if (ans.equals("n")) {
                return null;
            }
            System.out.println("Введите 'y' или 'n'");
        }
    }

    private static String inputScreenwriterName() {
        while (true) {
            System.out.print("Введите имя режиссера: ");
            String name = scanner.nextLine().trim();
            if (!name.isEmpty()) return name;
            System.out.println("Ошибка, имя не может быть пустым");
        }
    }

    private static Integer inputOscarCount() {
        while (true) {
            System.out.print("Введите количество оскаров у фильма, если хотите пропустить, то введите отрицательное число: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return null;
            if (isInteger(input)) {
                int res = Integer.parseInt(input);
                return res >= 0 ? res : null;
            }
            System.out.println("Ошибка, это не целое число");
        }
    }

    public static Location inputLocation() {
        while (true) {
            System.out.print("Хотите указать место рождения режиссера? (y/n): ");
            String ans = scanner.nextLine().trim().toLowerCase();
            if (ans.equals("n")) return null;
            if (!ans.equals("y")) {
                System.out.println("Введите 'y' или 'n'");
                continue;
            }

            long x;
            double y;
            int z;
            String name;

            while (true) {
                System.out.print("Введите x: ");
                String input = scanner.nextLine().trim();
                try {
                    x = Long.parseLong(input);
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка, x должен быть целым числом.");
                }
            }

            while (true) {
                System.out.print("Введите y локации (число с точкой): ");
                String input = scanner.nextLine().trim();
                if (isDouble(input)) {
                    y = Double.parseDouble(input.replace(",", "."));
                    break;
                }
                System.out.println("Ошибка, y должен быть числом");
            }

            while (true) {
                System.out.print("Введите z: ");
                String input = scanner.nextLine().trim();
                if (isInteger(input)) {
                    z = Integer.parseInt(input);
                    break;
                }
                System.out.println("Ошибка, z должен быть целым числом");
            }

            while (true) {
                System.out.print("Введите имя локации: ");
                name = scanner.nextLine().trim();
                if (!name.isEmpty()) break;
                System.out.println("Ошибка, имя локации не может быть пустым");
            }

            return new Location(x, y, z, name);
        }
    }

    private static Coordinates inputCoordinates() {
        System.out.println("Введите координаты:");
        String xStr, yStr;

        while (true) {
            System.out.print("x = ");
            xStr = scanner.nextLine().trim();
            if (isInteger(xStr)) break;
            System.out.println("Ошибка, это не число");
        }

        while (true) {
            System.out.print("y = ");
            yStr = scanner.nextLine().trim();
            if (isDouble(yStr)) break;
            System.out.println("Ошибка, это не число");
        }

        int x = Integer.parseInt(xStr);
        double y = Double.parseDouble(yStr.replace(",", "."));
        return new Coordinates(x, y);
    }

    private static <T> void printEnumDesc(Map<Integer, T> map) {
        map.forEach((i, e) -> System.out.printf("- %d: %s\n", i, e));
    }

    private static MovieGenre inputMovieGenre() {
        while (true) {
            System.out.println("Введите жанр:");
            printEnumDesc(MovieGenre.mapper);
            System.out.print("Жанр: ");
            String input = scanner.nextLine().trim();
            if (isInteger(input)) {
                int i = Integer.parseInt(input);
                if (MovieGenre.mapper.containsKey(i)) return MovieGenre.mapper.get(i);
            }
            System.out.println("Ошибка, выберите число из списка");
        }
    }

    private static MpaaRating inputMpaaRating() {
        while (true) {
            System.out.println("Введите рейтинг:");
            printEnumDesc(MpaaRating.mapper);
            System.out.print("Рейтинг: ");
            String input = scanner.nextLine().trim();
            if (isInteger(input)) {
                int i = Integer.parseInt(input);
                if (MpaaRating.mapper.containsKey(i)) return MpaaRating.mapper.get(i);
            }
            System.out.println("Ошибка, выберите число из списка");
        }
    }

    /**
     * Меню входа: вход по существующему логину либо регистрация нового
     * пользователя (п.5 задания). Логин и пароль запоминаются и потом
     * отправляются вместе с каждым запросом (п.10 задания).
     *
     * @return true, если пользователь успешно авторизовался
     */
    private boolean authorize() {
        while (true) {
            System.out.println("--- Вход в систему ---");
            System.out.println("1 - войти");
            System.out.println("2 - зарегистрироваться");
            System.out.println("3 - выход");
            System.out.print("Выберите пункт: ");

            String choice = scanner.nextLine().trim();
            if (choice.equals("3")) return false;
            if (!choice.equals("1") && !choice.equals("2")) {
                System.out.println("Введите 1, 2 или 3\n");
                continue;
            }

            System.out.print("Введите логин: ");
            String enteredLogin = scanner.nextLine().trim();
            System.out.print("Введите пароль: ");
            String enteredPassword = scanner.nextLine().trim();

            if (enteredLogin.isEmpty() || enteredPassword.isEmpty()) {
                System.out.println("Логин и пароль не могут быть пустыми!\n");
                continue;
            }

            CommandType type = choice.equals("2") ? CommandType.REGISTER : CommandType.AUTHENTICATE;
            CommandResponse response;
            try {
                response = client.sendRequest(new CommandRequest(
                        null, type, new ArrayList<>(), null, enteredLogin, enteredPassword));
            } catch (IOException e) {
                System.out.println("Сервер недоступен: " + e.getMessage());
                return false;
            }

            if (response.error() != null) {
                System.out.println("Ошибка: " + response.error());
                System.out.println("Попробуйте снова.\n");
                continue;
            }

            // Регистрация не выдаёт сессию, поэтому сразу выполняем вход
            // с теми же данными — так пользователь попадает в систему за один шаг.
            if (type == CommandType.REGISTER) {
                System.out.println(response.result());
                try {
                    response = client.sendRequest(new CommandRequest(
                            null, CommandType.AUTHENTICATE, new ArrayList<>(), null, enteredLogin, enteredPassword));
                } catch (IOException e) {
                    System.out.println("Сервер недоступен: " + e.getMessage());
                    return false;
                }
                if (response.error() != null) {
                    System.out.println("Ошибка: " + response.error() + "\n");
                    continue;
                }
            }

            this.login = enteredLogin;
            this.password = enteredPassword;
            System.out.println("Успешно! Добро пожаловать, " + this.login + "\n");
            System.out.println("Список доступных команд — help\n");
            return true;
        }
    }

    @Override
    public void run() {
        Scanner lineScanner;
        List<String> args = new ArrayList<>();

        // Пока пользователь не вошёл в систему, команды не отправляются
        // (п.7 задания: неавторизованным пользователям команды запрещены).
        if (!authorize()) return;

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine();
            if (line.isEmpty()) continue;
            lineScanner = new Scanner(line);

            String command = lineScanner.next();
            args.clear();
            while (lineScanner.hasNext()) args.add(lineScanner.next());

            if (commandMap.containsKey(command)) {
                try {
                    commandMap.get(command).exec().accept(args);
                } catch (CommandLineExitException e) {
                    return;
                }
            } else {
                System.out.println("Такой команды не существует");
                System.out.println("Посмотреть все команды можно при помощи help");
            }
        }
    }

    private static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) return false;
        return str.matches("-?\\d+");
    }

    private static boolean isDouble(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str.replace(",", "."));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
