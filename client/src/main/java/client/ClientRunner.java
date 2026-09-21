package client;

import client.cmd.CommandLine;

import java.io.IOException;
/**
 * Точка входа клиентского приложения.
 * Создаёт UDP-клиент и запускает командную строку.
 *
 * @param host хост сервера
 * @param port порт сервера
 */
public record ClientRunner(String host, int port) implements Runnable {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;

    /**
     * Запускает клиент: создаёт UDPClient и передаёт его в CommandLine.
     */
    @Override
    public void run() {
        try (UDPClient client = new UDPClient(host, port)) {
            new CommandLine(client).run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Точка входа клиентского приложения.
     *
     * @param args необязательные аргументы: хост и порт сервера
     *             (по умолчанию localhost:8080)
     */
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = DEFAULT_PORT;
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Порт должен быть числом, используется порт по умолчанию " + DEFAULT_PORT);
            }
        }
        new ClientRunner(host, port).run();
    }
}
