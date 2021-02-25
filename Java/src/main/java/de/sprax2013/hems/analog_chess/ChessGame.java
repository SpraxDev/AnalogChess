package de.sprax2013.hems.analog_chess;

import java.awt.Point;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ChessGame {
    public static final boolean DEBUG_IGNORE_TURNS = false;
    public static final int BOARD_SIZE = 8 * 8;

    private final ActiveChessman[] board = new ActiveChessman[BOARD_SIZE];
    private boolean whitesTurn = true;

    private int counter = 0;
    public ChessGame() {
        // Set default positions
        for (int i = 0; i < 2; i++) {
            boolean whiteMoving = i != 0;

            int y = (i == 0 ? 0 : 7);
            int yPawns = (i == 0 ? 1 : 6);

            int boardI = y * 8;

            this.board[boardI] = new ActiveChessman(Chessman.ROOK, whiteMoving);
            this.board[++boardI] = new ActiveChessman(Chessman.KNIGHT, whiteMoving);
            this.board[++boardI] = new ActiveChessman(Chessman.BISHOP, whiteMoving);
            this.board[++boardI] = new ActiveChessman(Chessman.QUEEN, whiteMoving);
            this.board[++boardI] = new ActiveChessman(Chessman.KING, whiteMoving);
            this.board[++boardI] = new ActiveChessman(Chessman.BISHOP, whiteMoving);
            this.board[++boardI] = new ActiveChessman(Chessman.KNIGHT, whiteMoving);
            this.board[++boardI] = new ActiveChessman(Chessman.ROOK, whiteMoving);

            for (int j = 0; j < 8; j++) {
                this.board[j + yPawns * 8] = new ActiveChessman(Chessman.PAWN, whiteMoving);
            }
        }
    }

    public ActiveChessman getChessmanAt(int index) {
        return this.board[index];
    }

    public boolean isWhitesTurn() {
        return this.whitesTurn;
    }

    public void moveChessman(int chessmanIndex, int chessmanTargetIndex) {
        if (chessmanIndex == chessmanTargetIndex) {
            throw new IllegalArgumentException("Tried to move a chessman to the location it's currently on");
        } else if (this.board[chessmanTargetIndex] != null && this.board[chessmanTargetIndex].type == Chessman.KING) {
            throw new IllegalArgumentException("Tried attacking a " + Chessman.KING);
        }

        ActiveChessman chessman = this.board[chessmanIndex];
        MoveType moveType = getPossibleMoves(chessmanIndex).get(chessmanTargetIndex);

        if (chessman == null) {
            throw new IllegalArgumentException((this.whitesTurn ? "White" : "Black") + " tried to move a non-existing chessman");
        } else if (!DEBUG_IGNORE_TURNS && chessman.whitesChessman != this.whitesTurn) {
            throw new IllegalArgumentException((this.whitesTurn ? "White" : "Black") + " tried to move an opposing chessman");
        } else if (moveType == null) {
            throw new IllegalArgumentException(chessman.type + " tried to make an illegal move (" + chessmanIndex + " -> " + chessmanTargetIndex + ")");
        }

        if (moveType == MoveType.CASTLING) {
            if (chessmanIndex < chessmanTargetIndex) {
                board[chessmanIndex + 1] = board[chessmanTargetIndex + 1];
                board[chessmanTargetIndex + 1] = null;
            } else {
                board[chessmanIndex - 2] = board[chessmanTargetIndex - 1];
                board[chessmanTargetIndex - 1] = null;
            }
        } else if (moveType == MoveType.EN_PASSANT) {
            // FIXME: Does not work correctly for a black pawn

            int v = this.whitesTurn ? 1 : -1;

            if (Math.abs(chessmanTargetIndex - chessmanIndex) == 7) {
                board[chessmanIndex + v] = null;
            } else {
                board[chessmanIndex - v] = null;
            }
        } else if (moveType == MoveType.PROMOTION) {
            chessman = new ActiveChessman(Chessman.QUEEN, chessman.whitesChessman);
        } else if (moveType == MoveType.UNDER_PROMOTION) {
            chessman = new ActiveChessman(Chessman.KNIGHT, chessman.whitesChessman);
        }

        if (chessman.type == Chessman.PAWN) {
            chessman.setDoublePawnMove(moveType == MoveType.PAWN_DOUBLE_MOVE);
        }

        board[chessmanIndex] = null;
        board[chessmanTargetIndex] = chessman;

        // TODO: Write to this.moves
        chessman.setMoved(counter);
        ++counter;

        if (!DEBUG_IGNORE_TURNS) {
            this.whitesTurn = !this.whitesTurn;
        }

        // TODO: Check if any of the kings is being threatened and add checks that you can't make a move
        //  that results in your own king to be threatened
        //  check if the threatened king can make a move or the game is over
    }

    public int getKingIndex(boolean whitesKing) {
        for (int i = 0; i < this.board.length; ++i) {
            ActiveChessman chessman = this.board[i];

            if (chessman != null && chessman.whitesChessman == whitesKing && chessman.type == Chessman.KING) {
                return i;
            }
        }

        throw new IllegalStateException("No " + Chessman.KING + " could be found for " + (whitesKing ? "white" : "black"));
    }

    public int getKingInCheck() {
        for (int i = 0; i < 2; ++i) {
            int kingIndex = getKingIndex(i == 0);

            if (getFirstThreateningChessman(kingIndex) != -1) {
                return kingIndex;
            }
        }

        return -1;
    }

    public int getFirstThreateningChessman(int index) {
        for (int i = 0; i < this.board.length; ++i) {
            ActiveChessman chessman = this.board[i];

            if (chessman != null && getPossibleMoves(i).containsKey(index)) {
                return i;
            }
        }

        return -1;
    }

    public Map<Integer, MoveType> getPossibleMoves(int index) {
        Map<Integer, MoveType> result = new LinkedHashMap<>();

        ActiveChessman chessman = Objects.requireNonNull(getCachedChessman(index));

        final int x = index % 8;
        final int y = index / 8;

        // TODO: Put each chessman into own method

        AtomicReference<MoveType> forceQueen = new AtomicReference<>();
        Runnable bishop = () -> {
            final MoveType forcedMoveType = forceQueen.get();

            for (int i = 1; i <= 4; i++) {
                int tX = x;
                int tY = y;

                while (true) {

                    if (i == 1) {
                        ++tX;
                        ++tY;
                    } else if (i == 2) {
                        ++tX;
                        --tY;
                    } else if (i == 3) {
                        --tX;
                        ++tY;
                    } else {
                        --tX;
                        --tY;
                    }

                    if (isOutOfBounds(tX, tY)) break;

                    if (isOccupiedBy(tX, tY, chessman.whitesChessman)) {
                        break;
                    }
                    if (!isOccupied(tX, tY)) {
                        result.put(tX + (tY * 8), forcedMoveType == null ? MoveType.NORMAL : forcedMoveType);
                    } else if (isOccupiedBy(tX, tY, !chessman.whitesChessman)) {
                        result.put(tX + (tY * 8), forcedMoveType == null ? MoveType.ATTACK : forcedMoveType);
                        break;
                    }
                }
            }
        };

        AtomicReference<MoveType> forceKnight = new AtomicReference<>();
        Runnable knight = () -> {
            final MoveType forcedMoveType = forceKnight.get();

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 2; j++) {
                    int tX = x;
                    int tY = y;

                    if (i % 2 == 1) {
                        int diffX = i >= 2 ? 2 : 1;
                        int diffY = i >= 2 ? 1 : 2;

                        if (j == 1) {
                            diffX = -diffX;
                            diffY = -diffY;
                        }

                        tX = x + diffX;
                        tY = y + diffY;
                    } else {
                        int diffX = i >= 2 ? -2 : -1;
                        int diffY = i >= 2 ? 1 : 2;

                        if (j == 1) {
                            diffX = -diffX;
                            diffY = -diffY;
                        }

                        tX = x + diffX;
                        tY = y + diffY;
                    }

                    if (!isOutOfBounds(tX, tY)) {
                        if (!isOccupied(tX, tY)) {
                            result.put(tX + (tY * 8), forcedMoveType == null ? MoveType.NORMAL : forcedMoveType);
                        } else if (isOccupiedBy(tX, tY, !chessman.whitesChessman)) {
                            result.put(tX + (tY * 8), forcedMoveType == null ? MoveType.ATTACK : forcedMoveType);
                        }
                    }
                }
            }
        };

        Runnable rook = () -> {
            final MoveType forcedMoveType = forceQueen.get();

            int tX = x;
            int tY = y;

            boolean mode = true;
            while (true) {
                if (!mode && tX > x) tX = x;
                tX += mode ? 1 : -1;

                if (!isOutOfBounds(tX, tY)) {
                    if (!isOccupied(tX, tY)) {
                        result.put(tX + (tY * 8), forcedMoveType == null ? MoveType.NORMAL : forcedMoveType);
                    } else {
                        if (isOccupiedBy(tX + (tY * 8), !chessman.whitesChessman)) {   // occupied by enemy
                            result.put(tX + (tY * 8), forcedMoveType == null ? MoveType.ATTACK : forcedMoveType);
                        }

                        if (!mode) break;
                        mode = false;
                    }
                } else {
                    if (!mode) break;
                    mode = false;
                }
            }

            tX = x;
            mode = true;
            while (true) {
                if (!mode && tY > y) tY = y;
                tY += mode ? 1 : -1;

                if (!isOutOfBounds(tX, tY)) {
                    if (!isOccupied(tX, tY)) {
                        result.put(tX + (tY * 8), forcedMoveType == null ? MoveType.NORMAL : forcedMoveType);
                    } else {
                        if (isOccupiedBy(tX + (tY * 8), !chessman.whitesChessman)) {   // occupied by enemy
                            result.put(tX + (tY * 8), forcedMoveType == null ? MoveType.ATTACK : forcedMoveType);
                        }

                        if (!mode) break;
                        mode = false;
                    }
                } else {
                    if (!mode) break;
                    mode = false;
                }
            }
        };

        switch (chessman.type) {
            case PAWN:
                int yOffsetPawn = (chessman.whitesChessman ? -1 : 1);

                // 1 forward
                int tY = y + yOffsetPawn;
                if (!isOutOfBounds(x, tY) && !isOccupied(x, tY)) {
                    result.put(x + (tY * 8), MoveType.NORMAL);
                }

                // double move
                if (!chessman.hasMovedAtLeasOnce()) {
                    tY = y + (chessman.whitesChessman ? -2 : 2);

                    if (!isOutOfBounds(x, tY) && !isOccupied(x, tY)) {
                        result.put(x + (tY * 8), MoveType.PAWN_DOUBLE_MOVE);
                    }
                }

                for (int i = 0; i < 2; ++i) {
                    int tX = x + (i == 0 ? 1 : -1);
                    tY = y + yOffsetPawn;

                    if (!isOutOfBounds(tX, tY) && isOccupiedBy(tX, tY, !chessman.whitesChessman)) {
                        result.put(tX + (tY * 8), MoveType.ATTACK);
                    }
                }
                for (int i = 0; i < 2; ++i) {
                    int tX = x + (i == 0 ? 1 : -1);
                    tY = y;


                    if (!isOutOfBounds(tX, tY+yOffsetPawn) && !isOccupied(tX,tY+yOffsetPawn))
                    if (isOccupiedBy(tX, tY, !chessman.whitesChessman) && getCachedChessman(tX,tY).hasDoublePawnMove() && getCachedChessman(tX,tY).getLastCounter()==counter-1){
                        result.put(tX + ((tY+yOffsetPawn) * 8), MoveType.EN_PASSANT);
                    }
                }

              /*  int tField = index + (chessman.whitesChessman ? -7 : 9);
                if (!isOutOfBounds(tField) && !isOccupied(tField)) {
                    if (isOccupiedBy(index + 1, !chessman.whitesChessman) && getCachedChessman(index + 1).hasDoublePawnMove()) {
                        result.put(tField, MoveType.EN_PASSANT);
                    }
                }
                tField = index + (chessman.whitesChessman ? -9 : 7);
                if (!isOutOfBounds(tField) && !isOccupied(tField)) {
                    if (isOccupiedBy(index - 1, !chessman.whitesChessman) && getCachedChessman(index - 1).hasDoublePawnMove()) {
                        result.put(tField, MoveType.EN_PASSANT);
                    }
                }
*/
                if (y == 0 || y == 7) {
                    forceKnight.set(MoveType.UNDER_PROMOTION);
                    knight.run();

                    forceQueen.set(MoveType.PROMOTION);
                    bishop.run();
                    rook.run();
                }

                break;
            case KNIGHT:
                knight.run();

                break;
            case BISHOP:
                bishop.run();

                break;
            case ROOK:
                rook.run();

                break;
            case QUEEN:
                bishop.run();
                rook.run();

                break;
            case KING:
                Point[] toCheck = new Point[] {
                        new Point(x + 1, y), new Point(x - 1, y),   // left, right
                        new Point(x, y + 1), new Point(x, y - 1),   // above, below
                        new Point(x + 1, y + 1), new Point(x - 1, y + 1),   // diagonally
                        new Point(x + 1, y - 1), new Point(x - 1, y - 1),   // diagonally
                };

                if (!chessman.hasMovedAtLeasOnce() &&
                        !isOccupied(index + 1) &&
                        !getCachedChessman(index + 3).hasMovedAtLeasOnce()) {
                    result.put(index + 2, MoveType.CASTLING);
                }
                if (!chessman.hasMovedAtLeasOnce() &&
                        !isOccupied(index - 1) &&
                        !isOccupied(index - 2) &&
                        !isOccupied(index - 3) &&
                        getCachedChessman(index - 4) != null &&
                        !getCachedChessman(index - 4).hasMovedAtLeasOnce()) {
                    result.put(index - 3, MoveType.CASTLING);
                }

                for (Point p : toCheck) {
                    if (!isOutOfBounds(p.x, p.y)) {
                        if (isOccupied(p.x, p.y)) {
                            if (isOccupiedBy(p.x, p.y, !chessman.whitesChessman)) {   // occupied by enemy
                                result.put(p.x + (p.y * 8), MoveType.ATTACK);
                            }
                        } else {
                            result.put(p.x + (p.y * 8), MoveType.NORMAL);
                        }
                    }
                }

                break;
            default:
                throw new RuntimeException("Not implemented");
        }

        return result;
    }

    ActiveChessman getCachedChessman(int field) {
        return this.board[field];
    }

    ActiveChessman getCachedChessman(int x,int y) {
        return getCachedChessman(x + (y*8));
    }

    private boolean isOccupied(int field) {
        return this.board[field] != null;
    }

    private boolean isOccupied(int x, int y) {
        return isOccupied(x + (y * 8));
    }

    private boolean isOccupiedBy(int field, boolean whiteChessman) {
        ActiveChessman chessman = this.board[field];

        return chessman != null && chessman.whitesChessman == whiteChessman;
    }

    private boolean isOccupiedBy(int x, int y, boolean whiteChessman) {
        return isOccupiedBy(x + (y * 8), whiteChessman);
    }

    private boolean isOutOfBounds(int field) {
        return field < 0 || field > 63;
    }

    private boolean isOutOfBounds(int x, int y) {
        return x < 0 || x > 7 || y < 0 || y > 7;
    }
}