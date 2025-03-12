package com.battleship.common;

import java.io.Serializable;
import java.util.List;

public class ShipPlacement implements Serializable {
    private List<Ship> ships;

    public ShipPlacement(List<Ship> ships) {
        this.ships = ships;
    }

    public List<Ship> getShips() {
        return ships;
    }
}
