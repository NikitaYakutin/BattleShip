package com.battleship.server;

import com.battleship.common.Cell;
import com.battleship.common.Move;
import com.battleship.common.MoveResult;
import com.battleship.common.Ship;
import com.battleship.common.ShipPlacement;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class BattleshipServer {
    private static final int PORT = 12345;
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);
    // Для формирования пар игроков
    public static final List<ClientHandler> waitingClients = new ArrayList<>();

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
    Socket socket;
    ObjectInputStream input;
    ObjectOutputStream output;
    String playerName;
    GameSession gameSession;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void sendMessage(Object msg) {
        try {
            output.writeObject(msg);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input  = new ObjectInputStream(socket.getInputStream());
            playerName = socket.getInetAddress().toString();
            sendMessage("Добро пожаловать на сервер \"Морской бой\"!");

            // Логика формирования пары
            synchronized(BattleshipServer.waitingClients) {
                if (BattleshipServer.waitingClients.isEmpty()) {
                    BattleshipServer.waitingClients.add(this);
                    sendMessage("Ожидание соперника...");
                } else {
                    ClientHandler opponent = BattleshipServer.waitingClients.remove(0);
                    GameSession session = new GameSession(opponent.playerName, this.playerName);
                    this.gameSession = session;
                    opponent.gameSession = session;
                    session.setPlayerHandlers(opponent, this);
                    opponent.sendMessage("Соперник найден. Игра начинается!");
                    sendMessage("Соперник найден. Игра начинается!");
                }
            }

            Object request;
            while ((request = input.readObject()) != null) {
                if (request instanceof Move) {
                    Move move = (Move) request;
                    System.out.println("Получен ход от " + playerName + ": (" + move.getX() + ", " + move.getY() + ")");
                    if (gameSession != null) {
                        MoveResult result = gameSession.makeMove(playerName, move.getX(), move.getY());
                        gameSession.broadcastMoveResult(result);
                    }
                } else if (request instanceof ShipPlacement) {
                    ShipPlacement sp = (ShipPlacement) request;
                    if (gameSession != null) {
                        gameSession.setShips(playerName, sp.getShips());
                        sendMessage("Ваши корабли установлены.");
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка в обработчике клиента: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }
}

class GameSession {
    private String player1, player2;
    private final Cell[][] fieldPlayer1 = new Cell[10][10];
    private final Cell[][] fieldPlayer2 = new Cell[10][10];
    private String currentPlayer;
    private ClientHandler player1Handler;
    private ClientHandler player2Handler;

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

    // Устанавливает корабли для игрока (например, после получения ShipPlacement)
    public synchronized void setShips(String player, List<Ship> ships) {
        Cell[][] field = player.equals(player1) ? fieldPlayer1 : fieldPlayer2;
        // Сброс поля
        for (int i = 0; i < 10; i++)
            for (int j = 0; j < 10; j++)
                field[i][j].setShip(false);
        // Размещение кораблей по координатам и ориентации
        for (Ship ship : ships) {
            int type = ship.getType();
            int x = ship.getX();
            int y = ship.getY();
            String orientation = ship.getOrientation();
            for (int k = 0; k < type; k++) {
                if (orientation.equalsIgnoreCase("horizontal")) {
                    if (x + k < 10)
                        field[x + k][y].setShip(true);
                } else if (orientation.equalsIgnoreCase("vertical")) {
                    if (y + k < 10)
                        field[x][y + k].setShip(true);
                }
            }
        }
    }

    // Обрабатывает ход и возвращает результат
    public synchronized MoveResult makeMove(String player, int x, int y) {
        if (!player.equals(currentPlayer)) {
            return new MoveResult(MoveResult.ResultType.MISS, x, y, "Не ваш ход.");
        }
        Cell[][] opponentField = player.equals(player1) ? fieldPlayer2 : fieldPlayer1;
        Cell target = opponentField[x][y];
        if (target.isHit()) {
            return new MoveResult(MoveResult.ResultType.ALREADY_HIT, x, y, "Уже был выстрел.");
        }
        target.setHit(true);
        MoveResult.ResultType resType = target.isShip() ? MoveResult.ResultType.HIT : MoveResult.ResultType.MISS;
        currentPlayer = player.equals(player1) ? player2 : player1;
        return new MoveResult(resType, x, y, "Ход принят. Следующий ход: " + currentPlayer);
    }

    public void setPlayerHandlers(ClientHandler h1, ClientHandler h2) {
        this.player1Handler = h1;
        this.player2Handler = h2;
    }

    public void broadcastMoveResult(MoveResult result) {
        if (player1Handler != null) player1Handler.sendMessage(result);
        if (player2Handler != null) player2Handler.sendMessage(result);
    }
}
