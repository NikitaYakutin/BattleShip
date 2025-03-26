package com.battleship.client;


import javax.swing.*;
import java.awt.*;

public class GameButton extends JButton {
    private static final Color EMPTY_COLOR = new Color(220, 220, 255);
    private static final Color SHIP_COLOR = new Color(100, 100, 150);
    private static final Color HIT_COLOR = new Color(255, 100, 100);
    private static final Color MISS_COLOR = new Color(150, 200, 255);

    private int xPos;
    private int yPos;
    private int state; // 0 - пусто, 1 - корабль, 2 - попадание, 3 - промах

    public GameButton(int x, int y) {
        this.xPos = x;
        this.yPos = y;
        this.state = 0;

        // Настраиваем внешний вид кнопки
        setBackground(EMPTY_COLOR);
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(true);
        setOpaque(true);

        // Устанавливаем текст с координатами
        setText("");

        // Настраиваем границу
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
    }

    public int getXPos() {
        return xPos;
    }

    public int getYPos() {
        return yPos;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
        updateAppearance();
    }

    private void updateAppearance() {
        switch (state) {
            case 0: // Пусто
                setBackground(EMPTY_COLOR);
                setText("");
                break;

            case 1: // Корабль
                setBackground(SHIP_COLOR);
                setText("");
                break;

            case 2: // Попадание
                setBackground(HIT_COLOR);
                setText("X");
                setForeground(Color.WHITE);
                break;

            case 3: // Промах
                setBackground(MISS_COLOR);
                setText("•");
                setForeground(Color.DARK_GRAY);
                break;
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (state == 2) { // Попадание - рисуем перекрестие
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));

            int padding = 8;
            int width = getWidth();
            int height = getHeight();

            // Рисуем X
            g2d.drawLine(padding, padding, width - padding, height - padding);
            g2d.drawLine(width - padding, padding, padding, height - padding);

            g2d.dispose();
        } else if (state == 3) { // Промах - рисуем точку
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(Color.DARK_GRAY);

            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int radius = 5;

            g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            g2d.dispose();
        }
    }
}