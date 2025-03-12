package com.battleship.client;

import com.battleship.common.Move;

import java.io.*;
import java.net.*;

public class BattleshipClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public BattleshipClient(String serverIP, int serverPort) throws IOException {
        socket = new Socket(serverIP, serverPort);
        out = new ObjectOutputStream(socket.getOutputStream());
        in  = new ObjectInputStream(socket.getInputStream());
        // Запуск потока для получения сообщений от сервера
        new Thread(new Listener()).start();
    }

    // Метод отправки координат выстрела
    public void sendMove(int x, int y) {
        try {
            out.writeObject(new Move(x, y));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Listener implements Runnable {
        @Override
        public void run() {
            try {
                Object response;
                while ((response = in.readObject()) != null) {
                    // Обработка сообщений от сервера (например, результат хода)
                    System.out.println("Сообщение от сервера: " + response);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Ошибка получения данных от сервера: " + e.getMessage());
            }
        }
    }
}
