package com.battleship.server;

import com.battleship.common.Move;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private final GameSession session;
    private final String playerName;

    public ClientHandler(Socket socket, GameSession session, String playerName) {
        this.socket = socket;
        this.session = session;
        this.playerName = playerName;
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            output.writeObject("Добро пожаловать в игру, " + playerName);

            while (true) {
                Object request = input.readObject();

                if (request instanceof Move) {
                    Move move = (Move) request;

                    if (!session.getCurrentPlayer().equals(playerName)) {
                        output.writeObject("OPPONENT_TURN"); // Не твой ход
                        continue;
                    }

                    boolean hit = session.makeMove(playerName, move.getX(), move.getY());

                    // Отправляем обновленные данные обеим сторонам
                    output.writeObject("UPDATE_BOARD");
                    output.writeObject(session.getVisibleBoard(playerName));

                    if (hit) {
                        output.writeObject("HIT"); // Попадание
                    } else {
                        output.writeObject("MISS"); // Промах
                        session.getCurrentPlayer(); // Меняем очередь хода
                    }

                    // Сообщаем игроку, чей ход теперь
                    if (session.getCurrentPlayer().equals(playerName)) {
                        output.writeObject("YOUR_TURN");
                    } else {
                        output.writeObject("OPPONENT_TURN");
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка в клиенте: " + e.getMessage());
        }
    }
}
