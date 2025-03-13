package com.battleship.server;

import com.battleship.common.Cell;

public class GameSession {
    private final String player1, player2;
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

    public synchronized boolean makeMove(String player, int x, int y) {
        if (!player.equals(currentPlayer)) {
            return false; // Игрок не должен делать ход, если это не его очередь
        }

        // Обработка выстрела
        Cell[][] opponentField = player.equals(player1) ? fieldPlayer2 : fieldPlayer1;
        Cell target = opponentField[x][y];

        if (target.isHit()) {
            return false; // Уже попадали в эту клетку
        }

        target.setHit(true); // Отмечаем попадание
        currentPlayer = (player.equals(player1)) ? player2 : player1; // Переключение хода

        return true;
    }


    public synchronized String getCurrentPlayer() {
        return currentPlayer;
    }

    public Cell[][] getVisibleBoard(String player) {
        Cell[][] field = player.equals(player1) ? fieldPlayer2 : fieldPlayer1;
        Cell[][] maskedField = new Cell[10][10];

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (field[i][j].isHit()) {
                    maskedField[i][j] = field[i][j]; // Отображаем только открытые клетки
                } else {
                    maskedField[i][j] = new Cell(i, j); // Остальные скрываем
                }
            }
        }
        return maskedField;
    }
}
