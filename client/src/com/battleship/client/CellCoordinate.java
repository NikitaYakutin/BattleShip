package com.battleship.client;

public class CellCoordinate {
    private final int x, y;
    public CellCoordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public int getX() { return x; }
    public int getY() { return y; }
}
