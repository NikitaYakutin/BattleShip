package com.battleship.client;

import com.battleship.common.Cell;
import com.battleship.common.Ship;
import com.battleship.server.GameData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class BoardPanel extends JPanel {
    private static final int CELL_SIZE = 40;
    private static final int GRID_SIZE = 10;
    private static final int BOARD_SIZE = CELL_SIZE * GRID_SIZE;
    private static final int MARGIN = 50;

    private int[][] playerBoard;
    private int[][] opponentBoard;
    private GameButton[][] playerButtons;
    private GameButton[][] opponentButtons;

    private JLabel statusLabel;
    private JLabel playerScoreLabel;
    private JLabel opponentScoreLabel;
    private JButton resetButton;

    private BattleshipGUI game;
    private AtomicBoolean playerTurn = new AtomicBoolean(false);
    private boolean placingShips = true;
    private boolean gameActive = false;

    private int[] shipSizes = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1}; // Размеры кораблей
    private int currentShipIndex = 0;
    private boolean isHorizontal = true; // Ориентация корабля при размещении

    public BoardPanel(BattleshipGUI game) {
        this.game = game;
        this.playerBoard = new int[GRID_SIZE][GRID_SIZE];
        this.opponentBoard = new int[GRID_SIZE][GRID_SIZE];
        this.playerButtons = new GameButton[GRID_SIZE][GRID_SIZE];
        this.opponentButtons = new GameButton[GRID_SIZE][GRID_SIZE];

        setLayout(null); // Используем абсолютное позиционирование
        setPreferredSize(new Dimension(2 * BOARD_SIZE + 3 * MARGIN, BOARD_SIZE + 3 * MARGIN));

        initializeComponents();
        createBoards();
        addKeyListener();
    }

    private void initializeComponents() {
        // Метки для досок
        JLabel playerBoardLabel = new JLabel("Ваше поле", SwingConstants.CENTER);
        playerBoardLabel.setBounds(MARGIN, 10, BOARD_SIZE, 30);
        add(playerBoardLabel);

        JLabel opponentBoardLabel = new JLabel("Поле противника", SwingConstants.CENTER);
        opponentBoardLabel.setBounds(BOARD_SIZE + 2 * MARGIN, 10, BOARD_SIZE, 30);
        add(opponentBoardLabel);

        // Информационные метки
        statusLabel = new JLabel("Разместите корабли. [R] для поворота", SwingConstants.CENTER);
        statusLabel.setBounds(MARGIN, BOARD_SIZE + MARGIN + 10, 2 * BOARD_SIZE + MARGIN, 30);
        add(statusLabel);

        playerScoreLabel = new JLabel("Ваш счет: 0", SwingConstants.LEFT);
        playerScoreLabel.setBounds(MARGIN, BOARD_SIZE + MARGIN + 40, BOARD_SIZE, 30);
        add(playerScoreLabel);

        opponentScoreLabel = new JLabel("Счет противника: 0", SwingConstants.RIGHT);
        opponentScoreLabel.setBounds(BOARD_SIZE + MARGIN, BOARD_SIZE + MARGIN + 40, BOARD_SIZE, 30);
        add(opponentScoreLabel);

        // Кнопка сброса
        resetButton = new JButton("Начать заново");
        resetButton.setBounds((2 * BOARD_SIZE + 3 * MARGIN) / 2 - 75, BOARD_SIZE + MARGIN + 70, 150, 30);
        resetButton.addActionListener(e -> resetGame());
        add(resetButton);
    }

    private void createBoards() {
        // Создаем поле игрока
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                GameButton button = new GameButton(x, y);
                button.setBounds(MARGIN + x * CELL_SIZE, MARGIN + y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                button.addActionListener(e -> handlePlayerBoardClick(button.getXPos(), button.getYPos()));
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (placingShips) {
                            showShipPlacementPreview(button.getXPos(), button.getYPos());
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (placingShips) {
                            clearShipPlacementPreview();
                        }
                    }
                });
                playerButtons[y][x] = button;
                add(button);
            }
        }

        // Создаем поле противника
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                GameButton button = new GameButton(x, y);
                button.setBounds(BOARD_SIZE + 2 * MARGIN + x * CELL_SIZE, MARGIN + y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                button.setEnabled(false); // Изначально поле противника неактивно
                button.addActionListener(e -> handleOpponentBoardClick(button.getXPos(), button.getYPos()));
                opponentButtons[y][x] = button;
                add(button);
            }
        }
    }

    private void addKeyListener() {
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_R && placingShips) {
                    // Меняем ориентацию корабля при нажатии R
                    isHorizontal = !isHorizontal;
                    statusLabel.setText("Разместите корабли. [R] для поворота. Ориентация: " +
                            (isHorizontal ? "горизонтальная" : "вертикальная"));
                    repaint();
                }
            }
        });
    }

    private void handlePlayerBoardClick(int x, int y) {
        if (placingShips) {
            placeShip(x, y);
        }
    }

    private void handleOpponentBoardClick(int x, int y) {
        if (gameActive && playerTurn.get() && opponentBoard[y][x] == 0) {
            // Отправляем ход на сервер
            GameData moveData = new GameData();
            moveData.setGameState(GameData.GameState.PLAYER_TURN);
            moveData.setX(x);
            moveData.setY(y);

            game.sendMove(moveData);

            // Временно блокируем доску до получения ответа
            setOpponentBoardEnabled(false);
            statusLabel.setText("Ожидание ответа противника...");
        }
    }

    private void placeShip(int startX, int startY) {
        if (currentShipIndex >= shipSizes.length) {
            return; // Все корабли уже размещены
        }

        int shipSize = shipSizes[currentShipIndex];

        // Проверяем, можно ли разместить корабль
        if (!canPlaceShip(startX, startY, shipSize, isHorizontal)) {
            statusLabel.setText("Нельзя разместить корабль в этой позиции!");
            return;
        }

        // Размещаем корабль
        for (int i = 0; i < shipSize; i++) {
            int x = isHorizontal ? startX + i : startX;
            int y = isHorizontal ? startY : startY + i;

            playerBoard[y][x] = 1; // 1 означает корабль
            playerButtons[y][x].setState(1);
        }

        currentShipIndex++;

        // Проверяем, все ли корабли размещены
        if (currentShipIndex >= shipSizes.length) {
            finishShipPlacement();
        } else {
            statusLabel.setText("Разместите корабль длиной " + shipSizes[currentShipIndex] +
                    ". [R] для поворота. Ориентация: " +
                    (isHorizontal ? "горизонтальная" : "вертикальная"));
        }
    }

    private boolean canPlaceShip(int startX, int startY, int shipSize, boolean horizontal) {
        // Проверяем, не выходит ли корабль за границы
        if (horizontal) {
            if (startX + shipSize > GRID_SIZE) return false;
        } else {
            if (startY + shipSize > GRID_SIZE) return false;
        }

        // Проверяем, не пересекается ли корабль с другими кораблями и соблюдается ли дистанция
        for (int i = -1; i <= shipSize; i++) {
            for (int j = -1; j <= 1; j++) {
                int x = horizontal ? startX + i : startX + j;
                int y = horizontal ? startY + j : startY + i;

                if (x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE) {
                    if (playerBoard[y][x] == 1) return false;
                }
            }
        }

        return true;
    }

    private void showShipPlacementPreview(int startX, int startY) {
        if (currentShipIndex >= shipSizes.length) return;

        int shipSize = shipSizes[currentShipIndex];
        boolean canPlace = canPlaceShip(startX, startY, shipSize, isHorizontal);

        clearShipPlacementPreview();

        // Показываем предварительный просмотр
        for (int i = 0; i < shipSize; i++) {
            int x = isHorizontal ? startX + i : startX;
            int y = isHorizontal ? startY : startY + i;

            if (x < GRID_SIZE && y < GRID_SIZE) {
                if (canPlace) {
                    playerButtons[y][x].setBackground(new Color(100, 200, 100)); // Зеленый - можно разместить
                } else {
                    playerButtons[y][x].setBackground(new Color(200, 100, 100)); // Красный - нельзя разместить
                }
            }
        }
    }

    private void clearShipPlacementPreview() {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (playerBoard[y][x] == 0) {
                    playerButtons[y][x].setBackground(null); // Восстанавливаем исходный цвет
                }
            }
        }
    }

    private void finishShipPlacement() {
        placingShips = false;
        statusLabel.setText("Все корабли размещены. Ожидание противника...");

        // Подготавливаем и отправляем данные о размещении кораблей
        GameData shipData = new GameData();
        shipData.setGameState(GameData.GameState.PLACING_SHIPS);
        shipData.setPlayerBoard(playerBoard);

        game.sendMove(shipData);
    }

    public void updateGame(GameData data) {
        if (data == null) return;

        switch (data.getGameState()) {
            case GAME_STARTED:
                gameActive = true;
                playerTurn.set(data.isPlayerTurn());
                updateStatusMessage();
                setOpponentBoardEnabled(data.isPlayerTurn());
                break;

            case PLAYER_TURN:
                processMove(data.getX(), data.getY(), data.isPlayerTurn());
                playerTurn.set(true);
                updateStatusMessage();
                setOpponentBoardEnabled(true);
                break;

            case OPPONENT_TURN:
                processMove(data.getX(), data.getY(), !data.isPlayerTurn());
                playerTurn.set(false);
                updateStatusMessage();
                setOpponentBoardEnabled(false);
                break;

            case HIT:
                if (data.isPlayerTurn()) {
                    // Игрок попал в корабль противника
                    opponentBoard[data.getY()][data.getX()] = 2; // 2 - попадание
                    opponentButtons[data.getY()][data.getX()].setState(2);
                    statusLabel.setText("Вы попали! Ваш ход.");
                } else {
                    // Противник попал в корабль игрока
                    playerBoard[data.getY()][data.getX()] = 2; // 2 - попадание
                    playerButtons[data.getY()][data.getX()].setState(2);
                    statusLabel.setText("Противник попал в ваш корабль!");
                }
                break;

            case MISS:
                if (data.isPlayerTurn()) {
                    // Игрок промахнулся
                    opponentBoard[data.getY()][data.getX()] = 3; // 3 - промах
                    opponentButtons[data.getY()][data.getX()].setState(3);
                    statusLabel.setText("Вы промахнулись. Ход противника.");
                } else {
                    // Противник промахнулся
                    playerBoard[data.getY()][data.getX()] = 3; // 3 - промах
                    playerButtons[data.getY()][data.getX()].setState(3);
                    statusLabel.setText("Противник промахнулся! Ваш ход.");
                }
                break;

            case SHIP_SUNK:
                if (data.isPlayerTurn()) {
                    statusLabel.setText("Вы потопили корабль противника!");
                } else {
                    statusLabel.setText("Противник потопил ваш корабль!");
                }
                break;

            case GAME_OVER:
                gameActive = false;
                if (data.isWinner()) {
                    statusLabel.setText("Поздравляем! Вы победили!");
                } else {
                    statusLabel.setText("Вы проиграли. Противник уничтожил все ваши корабли.");
                }
                setOpponentBoardEnabled(false);
                break;

            case OPPONENT_DISCONNECTED:
                gameActive = false;
                statusLabel.setText("Противник отключился. Вы победили!");
                setOpponentBoardEnabled(false);
                break;

            case ERROR:
                statusLabel.setText("Ошибка: " + data.getErrorMessage());
                break;
        }

        // Обновляем счет
        playerScoreLabel.setText("Ваш счет: " + data.getPlayerScore());
        opponentScoreLabel.setText("Счет противника: " + data.getOpponentScore());

        repaint();
    }

    private void processMove(int x, int y, boolean isPlayerMove) {
        if (isPlayerMove) {
            // Обновляем поле противника
            if (opponentBoard[y][x] == 0) {
                opponentBoard[y][x] = 3; // Предполагаем промах, сервер скорректирует если попадание
                opponentButtons[y][x].setState(3);
            }
        } else {
            // Обновляем поле игрока
            if (playerBoard[y][x] == 1) {
                playerBoard[y][x] = 2; // Попадание
                playerButtons[y][x].setState(2);
            } else if (playerBoard[y][x] == 0) {
                playerBoard[y][x] = 3; // Промах
                playerButtons[y][x].setState(3);
            }
        }
    }

    private void updateStatusMessage() {
        if (playerTurn.get()) {
            statusLabel.setText("Ваш ход! Выберите клетку на поле противника.");
        } else {
            statusLabel.setText("Ход противника. Ожидайте...");
        }
    }

    private void setOpponentBoardEnabled(boolean enabled) {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (opponentBoard[y][x] == 0) { // Только неатакованные клетки
                    opponentButtons[y][x].setEnabled(enabled);
                }
            }
        }
    }

    private void resetGame() {
        // Очищаем доски
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                playerBoard[y][x] = 0;
                opponentBoard[y][x] = 0;

                playerButtons[y][x].setState(0);
                playerButtons[y][x].setEnabled(true);

                opponentButtons[y][x].setState(0);
                opponentButtons[y][x].setEnabled(false);
            }
        }

        // Сбрасываем счетчики и флаги
        currentShipIndex = 0;
        placingShips = true;
        gameActive = false;
        playerTurn.set(false);

        // Обновляем интерфейс
        statusLabel.setText("Разместите корабли. [R] для поворота");
        playerScoreLabel.setText("Ваш счет: 0");
        opponentScoreLabel.setText("Счет противника: 0");

        // Уведомляем игру о сбросе
        game.resetGame();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Рисуем сетку для доски игрока
        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i <= GRID_SIZE; i++) {
            // Горизонтальные линии
            g.drawLine(MARGIN, MARGIN + i * CELL_SIZE, MARGIN + BOARD_SIZE, MARGIN + i * CELL_SIZE);
            // Вертикальные линии
            g.drawLine(MARGIN + i * CELL_SIZE, MARGIN, MARGIN + i * CELL_SIZE, MARGIN + BOARD_SIZE);
        }

        // Рисуем сетку для доски противника
        for (int i = 0; i <= GRID_SIZE; i++) {
            // Горизонтальные линии
            g.drawLine(BOARD_SIZE + 2 * MARGIN, MARGIN + i * CELL_SIZE,
                    BOARD_SIZE + 2 * MARGIN + BOARD_SIZE, MARGIN + i * CELL_SIZE);
            // Вертикальные линии
            g.drawLine(BOARD_SIZE + 2 * MARGIN + i * CELL_SIZE, MARGIN,
                    BOARD_SIZE + 2 * MARGIN + i * CELL_SIZE, MARGIN + BOARD_SIZE);
        }

        // Добавляем буквы и цифры для координат
        g.setFont(new Font("Arial", Font.BOLD, 14));

        String[] letters = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

        for (int i = 0; i < GRID_SIZE; i++) {
            // Буквы для доски игрока
            g.drawString(letters[i], MARGIN + i * CELL_SIZE + CELL_SIZE/2 - 5, MARGIN - 10);
            // Цифры для доски игрока
            g.drawString(Integer.toString(i+1), MARGIN - 20, MARGIN + i * CELL_SIZE + CELL_SIZE/2 + 5);

            // Буквы для доски противника
            g.drawString(letters[i], BOARD_SIZE + 2 * MARGIN + i * CELL_SIZE + CELL_SIZE/2 - 5, MARGIN - 10);
            // Цифры для доски противника
            g.drawString(Integer.toString(i+1), BOARD_SIZE + 2 * MARGIN - 20, MARGIN + i * CELL_SIZE + CELL_SIZE/2 + 5);
        }
    }
}