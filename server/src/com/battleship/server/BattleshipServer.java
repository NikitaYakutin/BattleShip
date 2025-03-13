package com.battleship.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class BattleshipServer {
    private static final int PORT = 12345;
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер запущен на порту " + PORT);

            GameSession session = null; // Создаем игровую сессию после подключения двух игроков
            String player1 = null;

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Игрок подключился: " + clientSocket.getInetAddress());

                if (player1 == null) {
                    player1 = "Игрок 1";
                    session = new GameSession(player1, "Игрок 2"); // Создаем новую игру
                    pool.execute(new ClientHandler(clientSocket, session, player1));
                } else {
                    pool.execute(new ClientHandler(clientSocket, session, "Игрок 2"));
                    player1 = null; // После подключения второго игрока обнуляем переменную
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
