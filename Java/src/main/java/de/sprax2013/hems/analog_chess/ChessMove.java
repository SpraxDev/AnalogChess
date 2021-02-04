package de.sprax2013.hems.analog_chess;

import org.jetbrains.annotations.NotNull;

import java.awt.Point;

public class ChessMove {
    private final boolean whiteMoving;

    private final @NotNull Chessman chessman;
    private final @NotNull Point targetField;
    private final @NotNull MoveType moveType;

    ChessMove(boolean whiteMoving, @NotNull Chessman chessman,
              @NotNull Point targetField, @NotNull MoveType moveType) {
        this.whiteMoving = whiteMoving;
        this.chessman = chessman;
        this.targetField = targetField;
        this.moveType = moveType;
    }

    public boolean isWhiteMoving() {
        return this.whiteMoving;
    }
}