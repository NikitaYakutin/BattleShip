package com.battleship.server;

import com.battleship.common.Cell;
import com.battleship.common.Move;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class BattleshipServer {
    private static final int PORT = 12345;
    // Хранение игровых сессий (ключ – идентификатор сессии или игрока)
    private static final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер запущен на порту " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    // Пример идентификации игрока (можно расширять)
    private String playerName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input  = new ObjectInputStream(socket.getInputStream());
            // Отправляем приветственное сообщение клиенту
            output.writeObject("Добро пожаловать на сервер \"Морской бой\"!");

            // Пример регистрации игрока и формирования сессии
            Object request;
            while ((request = input.readObject()) != null) {
                // Здесь можно реализовать протокол обмена сообщениями:
                // например, получать имя игрока, команду подключения, координаты выстрела и т.д.
                // Для примера рассмотрим случай, когда клиент отправляет объект типа Move.
                if (request instanceof Move) {
                    Move move = (Move) request;
                    System.out.println("Получен ход: (" + move.getX() + ", " + move.getY() + ")");
                    // Вызов проверки хода (валидность, попадание, смена игрока)
                    // Здесь можно найти соответствующую сессию и выполнить логику игры.
                    // После проверки отправляем результат обоим игрокам.
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка в обработчике клиента: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class GameSession {
    private String player1, player2;
    // Игровое поле представлено в виде двумерного массива.
    // Можно использовать специальный класс Cell для хранения информации о каждой ячейке.
    private final Cell[][] fieldPlayer1 = new Cell[10][10];
    private final Cell[][] fieldPlayer2 = new Cell[10][10];
    private String currentPlayer;

    public GameSession(String player1, String player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1;
        initializeField(fieldPlayer1);
        initializeField(fieldPlayer2);
    }

    private void initializeField(Cell[][] field) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                field[i][j] = new Cell(i, j);
            }
        }
    }

    // Метод проверки и применения хода.
    public synchronized boolean makeMove(String player, int x, int y) {
        if (!player.equals(currentPlayer)) {
            return false;
        }
        // Здесь можно добавить проверку координат и наличие уже совершённого выстрела.
        Cell[][] opponentField = player.equals(player1) ? fieldPlayer2 : fieldPlayer1;
        Cell target = opponentField[x][y];
        if (target.isHit()) {
            return false;
        }
        target.setHit(true);
        // Переключение хода
        currentPlayer = player.equals(player1) ? player2 : player1;
        // Дополнительно: проверка состояния кораблей и определения победителя.
        return true;
    }
}



