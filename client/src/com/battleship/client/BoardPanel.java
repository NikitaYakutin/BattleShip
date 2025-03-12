package com.battleship.client;

import com.battleship.common.Cell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Функциональный интерфейс для обратного вызова при выборе ячейки (для поля противника)
interface CellClickListener {
    void onCellClick(CellCoordinate coord);
}

class BoardPanel extends JPanel {
    private boolean isOwnBoard;
    private final int cellSize = 30;
    private final int gridSize = 10;
    private Cell[][] cells;
    private CellClickListener listener; // используется для поля противника

    // Конструктор для поля собственного расположения
    public BoardPanel(boolean isOwnBoard) {
        this(isOwnBoard, null);
    }

    // Конструктор для поля противника с обработчиком кликов
    public BoardPanel(boolean isOwnBoard, CellClickListener listener) {
        this.isOwnBoard = isOwnBoard;
        this.listener = listener;
        cells = new Cell[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                cells[i][j] = new Cell(i, j);
            }
        }
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX() / cellSize;
                int y = e.getY() / cellSize;
                if (!isOwnBoard && listener != null) {
                    listener.onCellClick(new CellCoordinate(x, y));
                }
            }
        });
        setPreferredSize(new Dimension(cellSize * gridSize, cellSize * gridSize));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Рисуем сетку и заполняем ячейки в зависимости от состояния
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                int drawX = i * cellSize;
                int drawY = j * cellSize;
                g.drawRect(drawX, drawY, cellSize, cellSize);
                if (cells[i][j].isHit()) {
                    g.setColor(Color.RED);
                    g.fillRect(drawX, drawY, cellSize, cellSize);
                    g.setColor(Color.BLACK);
                } else if (isOwnBoard && cells[i][j].isShip()) {
                    g.setColor(Color.GRAY);
                    g.fillRect(drawX, drawY, cellSize, cellSize);
                    g.setColor(Color.BLACK);
                }
            }
        }
    }
}

// Вспомогательный класс для передачи координат ячейки
class CellCoordinate {
    private final int x, y;
    public CellCoordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public int getX() { return x; }
    public int getY() { return y; }
}
