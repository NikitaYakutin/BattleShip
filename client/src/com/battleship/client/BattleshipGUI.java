package com.battleship.client;

import javax.swing.*;
import java.awt.*;

public class BattleshipGUI extends JFrame {
    private BoardPanel ownBoard;
    private BoardPanel enemyBoard;
    private JButton connectButton;
    private JTextArea statusArea;
    private BattleshipClient client;

    public BattleshipGUI() {
        setTitle("Клиент: Морской бой");
        setSize(650, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        ownBoard = new BoardPanel(true);
        enemyBoard = new BoardPanel(false, move -> {
            // При клике по полю противника отправляем ход на сервер
            if (client != null) {
                client.sendMove(move.getX(), move.getY());
            }
        });

        JPanel boardsPanel = new JPanel(new GridLayout(1, 2));
        boardsPanel.add(ownBoard);
        boardsPanel.add(enemyBoard);
        add(boardsPanel, BorderLayout.CENTER);

        statusArea = new JTextArea(5, 20);
        statusArea.setEditable(false);
        add(new JScrollPane(statusArea), BorderLayout.SOUTH);

        connectButton = new JButton("Подключиться к серверу");
        connectButton.addActionListener(e -> connectToServer());
        add(connectButton, BorderLayout.NORTH);
    }

    private void connectToServer() {
        try {
            // Здесь можно задать IP и порт через диалог или задать константами
            client = new BattleshipClient("localhost", 12345);
            statusArea.append("Подключено к серверу.\n");
        } catch (Exception ex) {
            statusArea.append("Ошибка подключения: " + ex.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BattleshipGUI gui = new BattleshipGUI();
            gui.setVisible(true);
        });
    }
}
