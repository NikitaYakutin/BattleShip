
package com.battleship.client;

import com.battleship.common.Cell;

import javax.swing.*;
        import java.awt.*;
        import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

interface CellClickListener {
    void onCellClick(CellCoordinate coord);
}

public class BoardPanel extends JPanel {
    private boolean isOwnBoard;
    private final int cellSize = 30;
    private final int gridSize = 10;
    Cell[][] cells;
    private CellClickListener listener;

    public BoardPanel(boolean isOwnBoard) {
        this(isOwnBoard, null);
    }

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

    public void updateCell(int x, int y, boolean hit) {
        if (x < gridSize && y < gridSize) {
            cells[x][y].setHit(hit);
            repaint();
        }
    }

    public void setShips(java.util.List<com.battleship.common.Ship> ships) {
        // Сброс поля
        for (int i = 0; i < gridSize; i++)
            for (int j = 0; j < gridSize; j++)
                cells[i][j].setShip(false);
        // Размещение кораблей согласно конфигурации
        for (com.battleship.common.Ship ship : ships) {
            int type = ship.getType();
            int x = ship.getX();
            int y = ship.getY();
            String orientation = ship.getOrientation();
            for (int k = 0; k < type; k++) {
                if (orientation.equalsIgnoreCase("horizontal")) {
                    if (x + k < gridSize)
                        cells[x + k][y].setShip(true);
                } else if (orientation.equalsIgnoreCase("vertical")) {
                    if (y + k < gridSize)
                        cells[x][y + k].setShip(true);
                }
            }
        }
        repaint();
    }
}



