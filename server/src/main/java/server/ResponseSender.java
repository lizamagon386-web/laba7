package server;

import transport.CommandResponse;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Модуль отправки ответов клиенту по протоколу UDP.
 * Использует DatagramSocket (датаграммы) на стороне сервера.
 * Сериализация выполняется напрямую через ObjectOutputStream.
 */
public class ResponseSender {
    private final DatagramSocket socket;

    /**
     * Создаёт отправитель ответов на указанном сокете.
     *
     * @param socket UDP-сокет для отправки данных
     */
    public ResponseSender(DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     * Сериализует и отправляет ответ клиенту по UDP.
     *
     * @param response ответ для отправки
     * @param address  адрес клиента
     * @param port     порт клиента
     * @throws IOException если произошла ошибка при отправке
     */
    public void send(CommandResponse response, InetAddress address, int port) throws IOException {
        byte[] payload;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(response);
            oos.flush();
            payload = bos.toByteArray();
        }
        DatagramPacket packet = new DatagramPacket(payload, payload.length, address, port);
        socket.send(packet);
    }
}