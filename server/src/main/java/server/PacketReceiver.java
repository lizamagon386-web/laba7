package server;

import transport.CommandRequest;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Модуль приёма пакетов по протоколу UDP.
 * Использует DatagramSocket (датаграммы) на стороне сервера.
 * Десериализация выполняется напрямую через ObjectInputStream.
 */
public class PacketReceiver {
    private static final int PACKET_SIZE = 64000;
    private final DatagramSocket socket;

    /**
     * Создаёт приёмник пакетов на указанном сокете.
     *
     * @param socket UDP-сокет для приёма данных
     */
    public PacketReceiver(DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     * Запись, содержащая полученный запрос и адрес отправителя.
     *
     * @param request десериализованный запрос
     * @param address адрес клиента
     * @param port    порт клиента
     */
    public record ReceivedPacket(CommandRequest request, InetAddress address, int port) {}

    /**
     * Ожидает и принимает UDP-датаграмму, десериализует её в CommandRequest.
     *
     * @return полученный пакет с запросом и адресом клиента
     * @throws IOException            если произошла ошибка при получении данных
     * @throws ClassNotFoundException если не удалось десериализовать запрос
     */
    public ReceivedPacket receive() throws IOException, ClassNotFoundException {
        byte[] buffer = new byte[PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        byte[] payload = Arrays.copyOf(packet.getData(), packet.getLength());

        try (ByteArrayInputStream bis = new ByteArrayInputStream(payload);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            CommandRequest request = (CommandRequest) ois.readObject();
            return new ReceivedPacket(request, packet.getAddress(), packet.getPort());
        }
    }
}