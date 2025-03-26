package com.battleship.server;

import com.battleship.common.Cell;

import java.io.Serializable;
import java.util.Arrays;

public class GameData implements Serializable {
    private static final long serialVersionUID = 1L; // Версионирование для безопасной сериализации

    // Перечисление возможных состояний игры
    public enum GameState {
        WAITING_FOR_OPPONENT,
        GAME_STARTED,
        PLACING_SHIPS,
        PLAYER_TURN,
        OPPONENT_TURN,
        HIT,
        MISS,
        SHIP_SUNK,
        GAME_OVER,
        OPPONENT_DISCONNECTED,
        ERROR
    }

    private GameState gameState;
    private boolean playerTurn;
    private int[][] playerBoard; // 0 - пусто, 1 - корабль, 2 - попадание, 3 - промах
    private int[][] opponentBoard; // 0 - неизвестно, 2 - попадание, 3 - промах
    private int x; // Координаты последнего хода
    private int y;
    private boolean isError;
    private String errorMessage;
    private boolean winner; // true если этот игрок победил
    private int playerScore;
    private int opponentScore;
    private long timestamp; // Временная метка для отслеживания порядка сообщений

    public GameData() {
        this.gameState = GameState.WAITING_FOR_OPPONENT;
        this.playerBoard = new int[10][10];
        this.opponentBoard = new int[10][10];
        this.isError = false;
        this.timestamp = System.currentTimeMillis();
    }

    // Глубокое копирование для предотвращения изменений данных после отправки
    public GameData copy() {
        GameData copy = new GameData();
        copy.gameState = this.gameState;
        copy.playerTurn = this.playerTurn;

        for(int i = 0; i < 10; i++) {
            copy.playerBoard[i] = Arrays.copyOf(this.playerBoard[i], 10);
            copy.opponentBoard[i] = Arrays.copyOf(this.opponentBoard[i], 10);
        }

        copy.x = this.x;
        copy.y = this.y;
        copy.isError = this.isError;
        copy.errorMessage = this.errorMessage;
        copy.winner = this.winner;
        copy.playerScore = this.playerScore;
        copy.opponentScore = this.opponentScore;
        copy.timestamp = System.currentTimeMillis(); // Обновляем timestamp при копировании

        return copy;
    }

    // Геттеры и сеттеры
    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public boolean isPlayerTurn() {
        return playerTurn;
    }

    public void setPlayerTurn(boolean playerTurn) {
        this.playerTurn = playerTurn;
    }

    public int[][] getPlayerBoard() {
        return playerBoard;
    }

    public void setPlayerBoard(int[][] playerBoard) {
        for(int i = 0; i < 10; i++) {
            this.playerBoard[i] = Arrays.copyOf(playerBoard[i], 10);
        }
    }

    public int[][] getOpponentBoard() {
        return opponentBoard;
    }

    public void setOpponentBoard(int[][] opponentBoard) {
        for(int i = 0; i < 10; i++) {
            this.opponentBoard[i] = Arrays.copyOf(opponentBoard[i], 10);
        }
    }

    public void updateCell(int x, int y, int value, boolean isPlayerBoard) {
        if (x >= 0 && x < 10 && y >= 0 && y < 10) {
            if (isPlayerBoard) {
                playerBoard[y][x] = value;
            } else {
                opponentBoard[y][x] = value;
            }
        }
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        if (errorMessage != null && !errorMessage.isEmpty()) {
            this.isError = true;
        }
    }

    public boolean isWinner() {
        return winner;
    }

    public void setWinner(boolean winner) {
        this.winner = winner;
        if (winner) {
            this.gameState = GameState.GAME_OVER;
        }
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public void setPlayerScore(int playerScore) {
        this.playerScore = playerScore;
    }

    public int getOpponentScore() {
        return opponentScore;
    }

    public void setOpponentScore(int opponentScore) {
        this.opponentScore = opponentScore;
    }

    public long getTimestamp() {
        return timestamp;
    }
}