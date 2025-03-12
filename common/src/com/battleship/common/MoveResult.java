package com.battleship.common;


import java.io.Serializable;

public class MoveResult implements Serializable {
    public enum ResultType { HIT, MISS, ALREADY_HIT, WIN }

    private ResultType resultType;
    private int x;
    private int y;
    private String message;

    public MoveResult(ResultType resultType, int x, int y, String message) {
        this.resultType = resultType;
        this.x = x;
        this.y = y;
        this.message = message;
    }

    public ResultType getResultType() { return resultType; }
    public int getX() { return x; }
    public int getY() { return y; }
    public String getMessage() { return message; }
}
