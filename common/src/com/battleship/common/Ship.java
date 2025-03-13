package com.battleship.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Ship implements Serializable {
    private final int type; // Длина корабля (например, 1 - подлодка, 4 - линкор)
    private final int x, y; // Начальная координата корабля (левый верхний угол)
    private final String orientation; // "horizontal" или "vertical"

    public Ship(int type, int x, int y, String orientation) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.orientation = orientation;
    }

    public int getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getOrientation() {
        return orientation;
    }

    /** Возвращает список клеток, занимаемых кораблем */
    public List<Cell> getCells() {
        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < type; i++) {
            if (orientation.equalsIgnoreCase("horizontal")) {
                cells.add(new Cell(x + i, y)); // Горизонтальное размещение
            } else {
                cells.add(new Cell(x, y + i)); // Вертикальное размещение
            }
        }
        return cells;
    }
}
