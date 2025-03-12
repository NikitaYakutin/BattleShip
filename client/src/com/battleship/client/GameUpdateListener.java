package com.battleship.client;

import com.battleship.common.MoveResult;

public interface GameUpdateListener {
    void updateGame(MoveResult result);
    void updateStatus(String status);
}
