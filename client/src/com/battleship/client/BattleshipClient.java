package com.battleship.client;

import com.battleship.common.Move;

import java.io.*;
import java.net.Socket;

public class BattleshipClient {
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private boolean myTurn = false;

    public BattleshipClient(String serverIP, int port) throws IOException {
        socket = new Socket(serverIP, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());

        new Thread(this::listenToServer).start();
    }

    private void listenToServer() {
        try {
            while (true) {
                Object response = input.readObject();

                if (response instanceof String) {
                    String message = (String) response;

                    switch (message) {
                        case "YOUR_TURN":
                            myTurn = true;
                            System.out.println("Ваш ход!");
                            break;
                        case "OPPONENT_TURN":
                            myTurn = false;
                            System.out.println("Ход противника...");
                            break;
                        case "HIT":
                            System.out.println("Попадание!");
                            break;
                        case "MISS":
                            System.out.println("Промах...");
                            break;
                        case "UPDATE_BOARD":
                            Object boardData = input.readObject();
                            updateBoard(boardData);
                            break;
                        default:
                            System.out.println(message);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка связи с сервером: " + e.getMessage());
        }
    }

    public void sendMove(int x, int y) {
        if (!myTurn) {
            System.out.println("Сейчас не ваш ход!");
            return;
        }
        try {
            output.writeObject(new Move(x, y));
            myTurn = false; // Ожидаем ответ от сервера
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateBoard(Object boardData) {
        // Здесь вызываем метод обновления доски в GUI
        System.out.println("Обновление игрового поля...");
    }
    public void sendMessage(Object message) {
        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException e) {
            System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
        }
    }

}
