package com.battleship.client;

import com.battleship.server.BattleshipServer;
import com.battleship.server.GameData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.*;

public class BattleshipGUI extends JFrame {
    private static final String TITLE = "Морской Бой";
    private static final int DEFAULT_PORT = 1234;

    private BattleshipClient client;
    private BattleshipServer server;
    private BoardPanel gamePanel;
    private ExecutorService networkExecutor;
    private ScheduledExecutorService gameUpdateExecutor;
    private volatile boolean connected = false;
    private volatile boolean isHost = false;

    public BattleshipGUI() {
        super(TITLE);

        networkExecutor = Executors.newSingleThreadExecutor();
        gameUpdateExecutor = Executors.newSingleThreadScheduledExecutor();

        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Создаем игровую панель
        gamePanel = new BoardPanel(this);
        add(gamePanel);

        // Создаем меню
        JMenuBar menuBar = new JMenuBar();

        JMenu gameMenu = new JMenu("Игра");
        JMenuItem hostItem = new JMenuItem("Создать игру");
        JMenuItem joinItem = new JMenuItem("Присоединиться к игре");
        JMenuItem disconnectItem = new JMenuItem("Отключиться");
        JMenuItem exitItem = new JMenuItem("Выход");

        hostItem.addActionListener(e -> showHostDialog());
        joinItem.addActionListener(e -> showJoinDialog());
        disconnectItem.addActionListener(e -> disconnect());
        exitItem.addActionListener(e -> System.exit(0));

        gameMenu.add(hostItem);
        gameMenu.add(joinItem);
        gameMenu.add(disconnectItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Помощь");
        JMenuItem rulesItem = new JMenuItem("Правила игры");
        JMenuItem aboutItem = new JMenuItem("О программе");

        rulesItem.addActionListener(e -> showRules());
        aboutItem.addActionListener(e -> showAbout());

        helpMenu.add(rulesItem);
        helpMenu.add(aboutItem);

        menuBar.add(gameMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        // Обработка закрытия окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private void showHostDialog() {
        JTextField portField = new JTextField(String.valueOf(DEFAULT_PORT));
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Введите порт для игры:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Создать игру",
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int port = Integer.parseInt(portField.getText().trim());
                if (port < 1024 || port > 65535) {
                    JOptionPane.showMessageDialog(this, "Порт должен быть между 1024 и 65535",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Запускаем сервер в отдельном потоке
                networkExecutor.submit(() -> {
                    try {
                        startServer(port);
                        // После запуска сервера подключаемся к нему как клиент
                        SwingUtilities.invokeLater(() -> {
                            startClient("localhost", port);
                        });
                    } catch (Exception e) {
                        final String errorMsg = e.getMessage();
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "Ошибка запуска сервера: " + errorMsg,
                                    "Ошибка", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                });

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Некорректный порт",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showJoinDialog() {
        JTextField hostField = new JTextField("localhost");
        JTextField portField = new JTextField(String.valueOf(DEFAULT_PORT));

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Введите адрес сервера:"));
        panel.add(hostField);
        panel.add(new JLabel("Введите порт:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Присоединиться к игре",
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String host = hostField.getText().trim();

            try {
                int port = Integer.parseInt(portField.getText().trim());
                if (port < 1 || port > 65535) {
                    JOptionPane.showMessageDialog(this, "Порт должен быть между 1 и 65535",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                startClient(host, port);

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Некорректный порт",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void startServer(int port) {
        if (server != null) {
            server.shutdown();
        }

        server = new BattleshipServer(port);
        server.start();
        isHost = true;
    }

    private void startClient(String host, String port) {
        try {
            int portNum = Integer.parseInt(port.trim());
            startClient(host, portNum);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Некорректный порт",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startClient(String host, int port) {
        if (connected) {
            disconnect();
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Запускаем подключение в отдельном потоке
        networkExecutor.submit(() -> {
            try {
                client = new BattleshipClient(host, port);
                boolean success = client.connect();

                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());

                    if (success) {
                        connected = true;
                        setTitle(TITLE + " - Подключено к " + host + ":" + port);

                        // Отправляем начальное состояние
                        GameData initialData = new GameData();
                        initialData.setGameState(GameData.GameState.WAITING_FOR_OPPONENT);
                        client.sendData(initialData);

                        // Запускаем обновление игры
                        startGameUpdates();

                        JOptionPane.showMessageDialog(this, "Подключение успешно!",
                                "Информация", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Не удалось подключиться к серверу",
                                "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    JOptionPane.showMessageDialog(this, "Ошибка подключения: " + errorMsg,
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void startGameUpdates() {
        // Останавливаем предыдущий исполнитель, если он существует
        if (gameUpdateExecutor != null && !gameUpdateExecutor.isShutdown()) {
            gameUpdateExecutor.shutdownNow();
        }

        gameUpdateExecutor = Executors.newSingleThreadScheduledExecutor();

        // Запускаем периодическое обновление игры каждые 100 мс
        gameUpdateExecutor.scheduleAtFixedRate(() -> {
            if (connected && client != null) {
                GameData data = client.receiveData();
                if (data != null) {
                    SwingUtilities.invokeLater(() -> {
                        gamePanel.updateGame(data);
                    });
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    public void sendMove(GameData moveData) {
        if (connected && client != null) {
            networkExecutor.submit(() -> {
                client.sendData(moveData);
            });
        }
    }

    public void resetGame() {
        if (connected) {
            // Отправляем сигнал о сбросе игры
            GameData resetData = new GameData();
            resetData.setGameState(GameData.GameState.WAITING_FOR_OPPONENT);
            sendMove(resetData);
        }
    }

    private void disconnect() {
        if (connected && client != null) {
            networkExecutor.submit(() -> {
                client.disconnect();

                SwingUtilities.invokeLater(() -> {
                    connected = false;
                    setTitle(TITLE);
                    JOptionPane.showMessageDialog(this, "Отключено от сервера",
                            "Информация", JOptionPane.INFORMATION_MESSAGE);
                });
            });
        }
    }

    private void shutdown() {
        // Останавливаем игровые обновления
        if (gameUpdateExecutor != null) {
            gameUpdateExecutor.shutdownNow();
        }

        // Отключаемся от сервера
        if (client != null) {
            client.disconnect();
        }

        // Останавливаем сервер, если мы хост
        if (isHost && server != null) {
            server.shutdown();
        }

        // Останавливаем исполнитель сетевых операций
        if (networkExecutor != null) {
            networkExecutor.shutdownNow();
        }
    }

    private void showRules() {
        String rules =
                "Правила игры 'Морской бой':\n\n" +
                        "1. В начале игры разместите ваши корабли на поле:\n" +
                        "   - 1 корабль длиной 4 клетки\n" +
                        "   - 2 корабля длиной 3 клетки\n" +
                        "   - 3 корабля длиной 2 клетки\n" +
                        "   - 4 корабля длиной 1 клетка\n\n" +
                        "2. Корабли не могут соприкасаться друг с другом даже углами.\n\n" +
                        "3. Игроки ходят по очереди, выбирая клетку на поле противника.\n\n" +
                        "4. Если игрок попадает в корабль противника, он получает право на дополнительный ход.\n\n" +
                        "5. Побеждает игрок, который первым потопит все корабли противника.";

        JOptionPane.showMessageDialog(this, rules, "Правила игры", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        String about =
                "Игра 'Морской бой'\n" +
                        "Версия 1.0\n\n" +
                        "© 2023 NikitaYakutin\n\n" +
                        "Сетевая игра \"Морской бой\" с возможностью\n" +
                        "игры по локальной сети или через интернет.";

        JOptionPane.showMessageDialog(this, about, "О программе", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        // Устанавливаем стиль интерфейса системы
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Запускаем игру в EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            BattleshipGUI game = new BattleshipGUI();
            game.setVisible(true);
        });
    }
}