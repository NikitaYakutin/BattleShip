package com.battleship.client;

import com.battleship.common.Move;
import com.battleship.common.MoveResult;

import java.io.*;
import java.net.*;
import javax.swing.SwingUtilities;

public class BattleshipClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private GameUpdateListener updateListener;

    public BattleshipClient(String serverIP, int serverPort, GameUpdateListener listener) throws IOException {
        this.updateListener = listener;
        socket = new Socket(serverIP, serverPort);
        out = new ObjectOutputStream(socket.getOutputStream());
        in  = new ObjectInputStream(socket.getInputStream());
        new Thread(new Listener()).start();
    }

    public void sendMove(int x, int y) {
        try {
            out.writeObject(new Move(x, y));
            out.flush();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void sendObject(Object obj) {
        try {
            out.writeObject(obj);
            out.flush();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private class Listener implements Runnable {
        @Override
        public void run() {
            try {
                Object response;
                while ((response = in.readObject()) != null) {
                    if (response instanceof MoveResult) {
                        final MoveResult result = (MoveResult) response; // объявляем финальную переменную
                        if (updateListener != null) {
                            SwingUtilities.invokeLater(() -> updateListener.updateGame(result));
                        }
                    } else if (response instanceof String) {
                        final String status = (String) response; // объявляем финальную переменную
                        if (updateListener != null) {
                            SwingUtilities.invokeLater(() -> updateListener.updateStatus(status));
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Ошибка получения данных от сервера: " + e.getMessage());
            }
        }
    }

}
