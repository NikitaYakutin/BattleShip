package com.battleship.client;

import com.battleship.common.Cell;
import com.battleship.common.Ship;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public class BoardPanel extends JPanel {
    private static final int GRID_SIZE = 10;
    private static final int CELL_SIZE = 30;
    private final boolean isOwnBoard;
    private final Cell[][] board;
    private final Consumer<Cell> moveHandler;

    public BoardPanel(boolean isOwnBoard) {
        this(isOwnBoard, null);
    }

    public BoardPanel(boolean isOwnBoard, Consumer<Cell> moveHandler) {
        this.isOwnBoard = isOwnBoard;
        this.moveHandler = moveHandler;
        this.board = new Cell[GRID_SIZE][GRID_SIZE];

        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                board[x][y] = new Cell(x, y);
            }
        }

        setPreferredSize(new Dimension(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE));

        if (!isOwnBoard) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int x = e.getX() / CELL_SIZE;
                    int y = e.getY() / CELL_SIZE;
                    if (moveHandler != null) {
                        moveHandler.accept(board[x][y]);
                    }
                }
            });
        }
    }

    /** Установка кораблей на доске */
    public void setShips(List<Ship> ships) {
        for (Ship ship : ships) {
            for (Cell cell : ship.getCells()) {
                board[cell.getX()][cell.getY()].setShip(true);
            }
        }
        repaint(); // Перерисовать доску после установки кораблей
    }

    /** Обновление ячейки после выстрела */
    public void updateCell(int x, int y, boolean hit) {
        board[x][y].setHit(hit);
        repaint(); // Перерисовать доску после изменения состояния
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                Cell cell = board[x][y];

                // Определение цвета в зависимости от состояния клетки
                if (cell.isHit()) {
                    g.setColor(cell.hasShip() ? Color.RED : Color.GRAY);
                } else if (isOwnBoard && cell.hasShip()) {
                    g.setColor(Color.BLUE);
                } else {
                    g.setColor(Color.WHITE);
                }

                g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                g.setColor(Color.BLACK);
                g.drawRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
    }
}
