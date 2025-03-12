package com.battleship.common;

import java.io.Serializable;

public class Cell implements Serializable {
    private final int x, y;
    private boolean isShip;
    private boolean isHit;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        this.isShip = false;
        this.isHit = false;
    }

    public boolean isShip() {
        return isShip;
    }

    public void setShip(boolean ship) {
        isShip = ship;
    }

    public boolean isHit() {
        return isHit;
    }

    public void setHit(boolean hit) {
        isHit = hit;
    }
}

class Ship {
    private final int type;
    private final int x, y;
    private final String orientation;

    public Ship(int type, int x, int y, String orientation) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.orientation = orientation;
    }

    public int getType() { return type; }
    public int getX() { return x; }
    public int getY() { return y; }
    public String getOrientation() { return orientation; }
}
