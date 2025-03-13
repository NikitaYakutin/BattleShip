package com.battleship.common;

import java.io.Serializable;

public class Cell implements Serializable {
    private final int x, y;
    private boolean isHit;
    private boolean hasShip; // Флаг наличия корабля

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        this.isHit = false;
        this.hasShip = false; // По умолчанию корабля нет
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isHit() {
        return isHit;
    }

    public void setHit(boolean hit) {
        this.isHit = hit;
    }

    public boolean hasShip() {
        return hasShip;
    }

    public void setShip(boolean hasShip) {
        this.hasShip = hasShip;
    }
}
