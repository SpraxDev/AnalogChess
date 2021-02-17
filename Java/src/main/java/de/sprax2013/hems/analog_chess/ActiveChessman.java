package de.sprax2013.hems.analog_chess;

import org.jetbrains.annotations.NotNull;

public class ActiveChessman {
    public final Chessman type;
    public final boolean whitesChessman;
    private boolean movedAtLeastOnce = false;
    private boolean doublePawnMove = false;

    public void setDoublePawnMove(boolean doublePawnMove) {
        this.doublePawnMove = doublePawnMove;
    }

    public ActiveChessman(@NotNull Chessman type, boolean whitesChessman) {
        this.type = type;
        this.whitesChessman = whitesChessman;
    }

    public boolean hasMovedAtLeasOnce() {
        return this.movedAtLeastOnce;
    }

    public boolean hasDoublePawnMove() {
        return this.doublePawnMove;
    }

    public void setMoved() {
        this.movedAtLeastOnce = true;
    }
}