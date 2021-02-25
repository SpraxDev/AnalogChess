package de.sprax2013.hems.analog_chess;

import org.jetbrains.annotations.NotNull;

public class ActiveChessman {
    public final Chessman type;
    public final boolean whitesChessman;

    private boolean movedAtLeastOnce = false;
    private boolean doublePawnMove = false;
    private int lastCounter = -1;

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

    public void setMoved(int currCounter) {
        this.movedAtLeastOnce = true;
        this.lastCounter=currCounter;
    }

    public int getLastCounter() {
        return this.lastCounter;
    }
}