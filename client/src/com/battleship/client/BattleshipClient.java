package com.battleship.client;

import com.battleship.server.GameData;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class BattleshipClient {
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String serverAddress;
    private int serverPort;
    private boolean connected = false;
    private final int CONNECTION_TIMEOUT = 10000; // 10 секунд
    private final int READ_TIMEOUT = 30000; // 30 секунд

    public BattleshipClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public boolean connect() {
        try {
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(serverAddress, serverPort), CONNECTION_TIMEOUT);
            clientSocket.setSoTimeout(READ_TIMEOUT);

            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(clientSocket.getInputStream());

            connected = true;
            System.out.println("Подключено к серверу: " + serverAddress + ":" + serverPort);
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка подключения к серверу: " + e.getMessage());
            cleanupConnection();
            return false;
        }
    }

    public boolean isConnected() {
        return connected && clientSocket != null && !clientSocket.isClosed();
    }

    public void sendData(GameData data) {
        if (!isConnected()) {
            System.err.println("Не удалось отправить данные: соединение отсутствует");
            return;
        }

        try {
            out.writeObject(data);
            out.flush();
            out.reset(); // Сбрасываем кеш объектов для предотвращения проблем с сериализацией
        } catch (IOException e) {
            System.err.println("Ошибка при отправке данных: " + e.getMessage());
            cleanupConnection();
        }
    }

    public GameData receiveData() {
        if (!isConnected()) {
            System.err.println("Не удалось получить данные: соединение отсутствует");
            return null;
        }

        try {
            return (GameData) in.readObject();
        } catch (SocketTimeoutException e) {
            System.err.println("Таймаут при ожидании данных от сервера");
            return null;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка при получении данных: " + e.getMessage());
            cleanupConnection();
            return null;
        }
    }

    public GameData receiveDataWithTimeout(long timeout) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeout;

        while (System.currentTimeMillis() < endTime) {
            if (!isConnected()) return null;

            try {
                if (in.available() > 0) {
                    return (GameData) in.readObject();
                }
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (Exception e) {
                System.err.println("Ошибка при получении данных с таймаутом: " + e.getMessage());
                cleanupConnection();
                return null;
            }
        }
        return null; // Таймаут истек
    }

    public void disconnect() {
        cleanupConnection();
        System.out.println("Отключено от сервера");
    }

    private void cleanupConnection() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
        } finally {
            in = null;
            out = null;
            clientSocket = null;
        }
    }
}