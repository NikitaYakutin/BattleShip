package com.battleship.common;

import java.io.Serializable;

public class Ship implements Serializable {
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
