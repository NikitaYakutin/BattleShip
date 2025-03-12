package com.battleship.client;

import com.battleship.common.FleetConfigParser;
import com.battleship.common.Ship;
import com.battleship.common.ShipPlacement;
import com.battleship.common.MoveResult;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BattleshipGUI extends JFrame implements GameUpdateListener {
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
            client = new BattleshipClient("localhost", 12345, this);
            statusArea.append("Подключено к серверу.\n");

            // Автоматическая расстановка кораблей через FleetConfigParser
            List<Ship> ships = FleetConfigParser.parseFleetConfig(getClass().getResource("/fleet.xml").getFile());
            // Обновляем собственное поле клиента
            ownBoard.setShips(ships);
            // Отправляем информацию о кораблях на сервер
            client.sendObject(new ShipPlacement(ships));

        } catch (Exception ex) {
            statusArea.append("Ошибка подключения: " + ex.getMessage() + "\n");
        }
    }

    @Override
    public void updateGame(MoveResult result) {
        // Обновляем поля – для демонстрации обновляем обе панели
        enemyBoard.updateCell(result.getX(), result.getY(), true);
        ownBoard.updateCell(result.getX(), result.getY(), true);
        statusArea.append(result.getMessage() + "\n");
    }

    @Override
    public void updateStatus(String status) {
        statusArea.append(status + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BattleshipGUI gui = new BattleshipGUI();
            gui.setVisible(true);
        });
    }
}
