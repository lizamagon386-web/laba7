package client;

import transport.CommandRequest;
import transport.CommandResponse;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

/**
 * UDP-клиент для взаимодействия с сервером.
 * Использует DatagramChannel (сетевой канал) в неблокирующем режиме.
 * Сериализация выполняется напрямую через ObjectOutputStream/ObjectInputStream.
 */
public class UDPClient implements Closeable {

    private static final int PACKET_SIZE = 64000;
    private static final int TIMEOUT = 5000;
    private static final int ATTEMPTS = 3;

    private final DatagramChannel channel;
    private final Selector selector;
    private final InetSocketAddress serverAddress;

    /**
     * Создаёт UDP-клиент и подключается к указанному серверу.
     *
     * @param host хост сервера
     * @param port порт сервера
     * @throws IOException если не удалось открыть канал
     */
    public UDPClient(String host, int port) throws IOException {
        serverAddress = new InetSocketAddress(host, port);
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
    }

    /**
     * Отправляет запрос на сервер и возвращает ответ.
     * При недоступности сервера повторяет попытку {@code ATTEMPTS} раз.
     *
     * @param r запрос для отправки
     * @return ответ от сервера
     * @throws IOException если сервер недоступен после всех попыток
     */
    public CommandResponse sendRequest(CommandRequest r) throws IOException {
        byte[] payload;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(r);
            oos.flush();
            payload = bos.toByteArray();
        }
        discardStaleResponses();

        ByteBuffer buffer = ByteBuffer.wrap(payload);
        for (int i = 1; i <= ATTEMPTS; i++) {
            buffer.rewind();
            channel.send(buffer, serverAddress);
            CommandResponse response = receive();
            if (response != null) {
                return response;
            }
        }
        throw new IOException("Не получилось установить связь с сервером");
    }
    /**
     * Выбрасывает запоздавшие ответы на предыдущие запросы (например, на повторную
     * отправку после таймаута), чтобы они не были приняты за ответ на новый запрос.
     */
    private void discardStaleResponses() throws IOException {
        ByteBuffer trash = ByteBuffer.allocate(PACKET_SIZE);
        while (channel.receive(trash) != null) {
            trash.clear();
        }
        selector.selectNow();
        selector.selectedKeys().clear();
    }

    /**
     * Ожидает ответ от сервера в течение {@code TIMEOUT} миллисекунд.
     *
     * @return ответ от сервера или null если истёк таймаут
     * @throws IOException если произошла ошибка при получении данных
     */
    private CommandResponse receive() throws IOException {
        int ready = selector.select(TIMEOUT);
        if (ready == 0) return null;

        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            if (!key.isReadable()) {
                continue;
            }

            ByteBuffer buffer = ByteBuffer.allocate(PACKET_SIZE);
            if (channel.receive(buffer) == null) {
                continue;
            }

            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                return (CommandResponse) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }
        return null;
    }

    /**
     * Закрывает селектор и канал.
     *
     * @throws IOException если произошла ошибка при закрытии
     */
    @Override
    public void close() throws IOException {
        selector.close();
        channel.close();
    }
}