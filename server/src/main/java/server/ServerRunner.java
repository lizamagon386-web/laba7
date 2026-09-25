package server;

import server.dao.MovieDao;
import domain.Movie;
import server.storage.MovieCollection;
import transport.CommandResponse;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Точка входа сервера.
 *
 * Многопоточность (требования задания):
 *  1. чтение запросов выполняется в отдельном потоке java.lang.Thread;
 *  2. на каждый полученный запрос создаётся новый поток для его обработки;
 *  3. на каждый готовый ответ создаётся новый поток для отправки;
 *  4. доступ к коллекции синхронизирован через ReentrantLock
 *     (см. {MovieCollection} и {@link CommandProcessor}).
 *
 * Чтение вынесено в один постоянный поток намеренно: DatagramSocket.receive()
 * блокирующий, и несколько потоков, одновременно читающих один и тот же сокет,
 * получали бы пакеты в непредсказуемом порядке. Параллелизм обеспечивается
 * тем, что обработка и отправка каждого запроса идут в своих потоках.
 */
public class ServerRunner implements Runnable {

    private static final Logger logger = Logger.getLogger(ServerRunner.class.getName());

    public static final int PORT = 8080;

    @Override
    public void run() {
        MovieCollection collection = new MovieCollection();
        DbManager dbManager = new DbManager();
        MovieDao movieDao = new MovieDao(dbManager);

        try {
            dbManager.checkConnection();
            logger.info("Загрузка коллекции из базы данных...");
            List<Movie> fromDb = movieDao.findAll();
            fromDb.forEach(collection::add);
            logger.info("Коллекция загружена. Элементов: " + fromDb.size());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Критическая ошибка: база данных недоступна. "
                    + "Проверьте настройки подключения "
                    + "и то, что схема из sql/schema.sql применена", e);
            return;
        }

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            socket.setSoTimeout(500);

            PacketReceiver receiver = new PacketReceiver(socket);
            CommandProcessor processor = new CommandProcessor(collection, dbManager, movieDao);
            ResponseSender sender = new ResponseSender(socket);

            logger.info("Сервер запущен на порту " + PORT);
            Thread readingThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        PacketReceiver.ReceivedPacket packet = receiver.receive();
                        Thread processingThread = new Thread(() -> {
                            CommandResponse response = processor.process(packet.request());
                            Thread sendingThread = new Thread(() -> {
                                try {
                                    sender.send(response, packet.address(), packet.port());
                                } catch (IOException e) {
                                    logger.severe("Ошибка отправки ответа: " + e.getMessage());
                                }
                            }, "response-sender");
                            sendingThread.start();
                        }, "request-processor");
                        processingThread.start();

                    } catch (java.net.SocketTimeoutException ignored) {
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Ошибка при обработке запроса", e);
                    }
                }
            }, "request-reader");
            readingThread.start();
            readingThread.join();

        } catch (SocketException e) {
            logger.log(Level.SEVERE, "Ошибка сокета", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Сервер прерван", e);
        }
    }

    public static void main(String[] args) {
        new ServerRunner().run();
    }
}
