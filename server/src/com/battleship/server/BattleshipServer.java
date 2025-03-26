package com.battleship.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class BattleshipServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private final int PORT;
    private int nextClientId = 1;

    public  BattleshipServer(int port) {
        this.PORT = port;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("Сервер запущен на порту: " + PORT);

            // Отдельный поток для мониторинга активных соединений
            threadPool.submit(this::monitorConnections);

            // Основной цикл принятия подключений
            threadPool.submit(() -> {
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        int clientId = nextClientId++;
                        ClientHandler handler = new ClientHandler(clientId, clientSocket);
                        clients.put(clientId, handler);
                        threadPool.submit(handler);
                        System.out.println("Подключен новый клиент #" + clientId);
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("Ошибка при принятии подключения: " + e.getMessage());
                        }
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("Ошибка запуска сервера: " + e.getMessage());
            shutdown();
        }
    }

    private void monitorConnections() {
        while (running) {
            try {
                // Проверяем все соединения каждые 5 секунд
                TimeUnit.SECONDS.sleep(5);

                // Удаляем отключенных клиентов
                Iterator<Map.Entry<Integer, ClientHandler>> it = clients.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Integer, ClientHandler> entry = it.next();
                    if (!entry.getValue().isConnected()) {
                        System.out.println("Клиент #" + entry.getKey() + " отключен, удаляем из списка");
                        it.remove();
                    }
                }

                // Проверяем, нужно ли сопоставить игроков в комнаты
                matchPlayers();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void matchPlayers() {
        // Находим клиентов, которые ожидают начала игры
        List<ClientHandler> waitingClients = new ArrayList<>();

        for (ClientHandler client : clients.values()) {
            if (client.isWaitingForGame()) {
                waitingClients.add(client);
            }
        }

        // Сопоставляем игроков по парам
        for (int i = 0; i < waitingClients.size() / 2; i++) {
            ClientHandler player1 = waitingClients.get(i * 2);
            ClientHandler player2 = waitingClients.get(i * 2 + 1);

            // Создаем игровую сессию и уведомляем игроков
            GameSession session = new GameSession(player1, player2);
            player1.setGameSession(session);
            player2.setGameSession(session);

            // Отправляем сообщение о начале игры
            GameData startData1 = new GameData();
            startData1.setGameState(GameData.GameState.GAME_STARTED);
            startData1.setPlayerTurn(true); // Первый игрок ходит первым

            GameData startData2 = new GameData();
            startData2.setGameState(GameData.GameState.GAME_STARTED);
            startData2.setPlayerTurn(false);

            player1.sendData(startData1);
            player2.sendData(startData2);

            System.out.println("Создана новая игровая сессия между клиентами #" +
                    player1.getClientId() + " и #" + player2.getClientId());
        }
    }

    public void shutdown() {
        running = false;
        System.out.println("Завершение работы сервера...");

        // Закрываем все клиентские соединения
        for (ClientHandler client : clients.values()) {
            client.disconnect();
        }
        clients.clear();

        // Закрываем серверный сокет
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии серверного сокета: " + e.getMessage());
        }

        // Останавливаем пул потоков
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        System.out.println("Сервер остановлен");
    }

    // Внутренний класс для обработки клиентских подключений
    private class ClientHandler implements Runnable {
        private final int clientId;
        private final Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private volatile boolean connected = true;
        private volatile boolean waitingForGame = true;
        private GameSession gameSession = null;

        public ClientHandler(int clientId, Socket socket) {
            this.clientId = clientId;
            this.clientSocket = socket;
            try {
                // Важно создавать потоки в таком порядке
                this.out = new ObjectOutputStream(socket.getOutputStream());
                this.out.flush();
                this.in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                System.err.println("Ошибка при инициализации потоков для клиента #" + clientId);
                connected = false;
            }
        }

        @Override
        public void run() {
            try {
                while (connected && running) {
                    try {
                        GameData data = (GameData) in.readObject();
                        processClientData(data);
                    } catch (EOFException | SocketException e) {
                        // Клиент отключился
                        break;
                    } catch (ClassNotFoundException e) {
                        System.err.println("Ошибка при десериализации объекта от клиента #" + clientId);
                    }
                }
            } catch (IOException e) {
                System.err.println("Ошибка при обработке данных от клиента #" + clientId + ": " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void processClientData(GameData data) {
            if (data == null) return;

            // Обработка данных в зависимости от состояния игры
            if (gameSession != null) {
                gameSession.handlePlayerMove(this, data);
            } else {
                // Обработка данных от клиента, который еще не в игре
                if (data.getGameState() == GameData.GameState.WAITING_FOR_OPPONENT) {
                    waitingForGame = true;
                }
            }
        }

        public boolean isConnected() {
            return connected && !clientSocket.isClosed();
        }

        public boolean isWaitingForGame() {
            return waitingForGame && isConnected();
        }

        public void setGameSession(GameSession session) {
            this.gameSession = session;
            this.waitingForGame = false;
        }

        public int getClientId() {
            return clientId;
        }

        public void sendData(GameData data) {
            if (!isConnected()) return;

            try {
                out.writeObject(data);
                out.flush();
                out.reset(); // Сбрасываем кеш объектов
            } catch (IOException e) {
                System.err.println("Ошибка при отправке данных клиенту #" + clientId);
                disconnect();
            }
        }

        public void disconnect() {
            connected = false;
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                System.err.println("Ошибка при закрытии соединения с клиентом #" + clientId);
            }
            System.out.println("Клиент #" + clientId + " отключен");
        }
    }

    // Класс для управления игровой сессией между двумя игроками
    private class GameSession {
        private final ClientHandler player1;
        private final ClientHandler player2;
        private boolean gameActive = true;

        public GameSession(ClientHandler player1, ClientHandler player2) {
            this.player1 = player1;
            this.player2 = player2;
        }

        public void handlePlayerMove(ClientHandler player, GameData moveData) {
            if (!gameActive) return;

            // Определяем второго игрока
            ClientHandler opponent = (player == player1) ? player2 : player1;

            // Проверяем, подключен ли противник
            if (!opponent.isConnected()) {
                GameData gameOverData = new GameData();
                gameOverData.setGameState(GameData.GameState.OPPONENT_DISCONNECTED);
                player.sendData(gameOverData);
                gameActive = false;
                return;
            }

            // Передаем ход противнику
            opponent.sendData(moveData);

            // Проверяем конец игры
            if (moveData.getGameState() == GameData.GameState.GAME_OVER) {
                gameActive = false;
            }
        }
    }

    public static void main(String[] args) {
        int port = 1234; // По умолчанию

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Неверный формат порта, используем порт по умолчанию: " + port);
            }
        }

        BattleshipServer server = new  BattleshipServer(port);
        server.start();

        // Обработка сигнала остановки сервера
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
    }
}