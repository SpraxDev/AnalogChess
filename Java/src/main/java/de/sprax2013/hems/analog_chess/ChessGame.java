package de.sprax2013.hems.analog_chess;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ChessGame {
    //    public final List<ChessMove> moves = new ArrayList<>(100);
    final ActiveChessman[] cachedBoard = new ActiveChessman[8 * 8];

    private boolean whitesTurn = true;

    public ChessGame() {
        // Set default positions
        for (int i = 0; i < 2; i++) {
            boolean whiteMoving = i != 0;

            int y = (i == 0 ? 0 : 7);
            int yPawns = (i == 0 ? 1 : 6);

            int boardI = y * 8;

            this.cachedBoard[boardI] = new ActiveChessman(Chessman.ROOK, whiteMoving);
            this.cachedBoard[++boardI] = new ActiveChessman(Chessman.KNIGHT, whiteMoving);
            this.cachedBoard[++boardI] = new ActiveChessman(Chessman.BISHOP, whiteMoving);
            this.cachedBoard[++boardI] = new ActiveChessman(Chessman.QUEEN, whiteMoving);
            this.cachedBoard[++boardI] = new ActiveChessman(Chessman.KING, whiteMoving);
            this.cachedBoard[++boardI] = new ActiveChessman(Chessman.BISHOP, whiteMoving);
            this.cachedBoard[++boardI] = new ActiveChessman(Chessman.KNIGHT, whiteMoving);
            this.cachedBoard[++boardI] = new ActiveChessman(Chessman.ROOK, whiteMoving);

            for (int j = 0; j < 8; j++) {
                this.cachedBoard[j + yPawns * 8] = new ActiveChessman(Chessman.PAWN, whiteMoving);
            }
        }
    }

    public boolean isWhitesTurn() {
        return this.whitesTurn;
    }

    public void moveChessman(boolean whiteMoving, int chessmanIndex, int chessmanTargetIndex) {
        ActiveChessman chessmen = getCachedChessman(chessmanIndex);

        if (this.whitesTurn != whiteMoving) {
            throw new IllegalArgumentException((whiteMoving ? "White" : "Black") + " tried to move a chessman during the opponent's turn");
        } else if (chessmen == null) {
            throw new IllegalArgumentException((whiteMoving ? "White" : "Black") + " tried to move a non-existing chessman");
        } else if (chessmen.whitesChessman != whiteMoving) {
            throw new IllegalArgumentException((whiteMoving ? "White" : "Black") + " tried to move an opposing chessman");
        } else if (!isValidMove(chessmanIndex, chessmanTargetIndex)) {
            throw new IllegalArgumentException("The Chessman (" + chessmen.type + ") at ( " + chessmanIndex +
                    " ) to ( " + chessmanTargetIndex + " ) tried to make an illegal move");
        }
        if (getPossibleMoves(chessmanIndex).get(chessmanTargetIndex) == MoveType.CASTLING) {
            if (chessmanIndex < chessmanTargetIndex) {
                cachedBoard[chessmanIndex + 1] = cachedBoard[chessmanTargetIndex + 1];
                cachedBoard[chessmanTargetIndex + 1] = null;
            } else {
                cachedBoard[chessmanIndex - 2] = cachedBoard[chessmanTargetIndex - 1];
                cachedBoard[chessmanTargetIndex - 1] = null;
            }
        }
        if (getPossibleMoves(chessmanIndex).get(chessmanTargetIndex) == MoveType.EN_PASSANT) {
            int v = whiteMoving ? 1 : -1;

            if (Math.abs(chessmanTargetIndex - chessmanIndex) == 7) {
                cachedBoard[chessmanIndex + v] = null;
            } else {
                cachedBoard[chessmanIndex - v] = null;
            }
        }

        if (chessmen.type == Chessman.PAWN && Math.abs(chessmanIndex - chessmanTargetIndex) == 16) {
            chessmen.setDoublePawnMove(true);
        } else {
            chessmen.setDoublePawnMove(false);
        }
        cachedBoard[chessmanIndex] = null;
        cachedBoard[chessmanTargetIndex] = chessmen;

        // TODO: Write to this.moves
        chessmen.setMoved();
        this.whitesTurn = !this.whitesTurn;

        // TODO: Check if any of the kings is being threatened and add checks that you can't make a move
        //  that results in your own king to be threatened
        //  check if the threatened king can make a move or the game is over
        //  see #getThreateningChessman
    }

    public int getThreateningChessman(int index) {
        for (int i = 0; i < this.cachedBoard.length; i++) {
            ActiveChessman ac = this.cachedBoard[i];

            if (ac != null && getPossibleMoves(i).containsKey(index)) {
                return i;
            }
        }

        return -1;
    }

    public Map<Integer, MoveType> getPossibleMoves(int index) {
        Map<Integer, MoveType> result = new LinkedHashMap<>();

        ActiveChessman chessman = Objects.requireNonNull(getCachedChessman(index));

        int x = index % 8;
        int y = index / 8;

        // TODO: Put each chessman into own method

        Runnable bishop = () -> {
            int tX = x;
            int tY = y;

            // TODO: cleanup
            while (true) {
                ++tX;
                ++tY;

                if (isOutOfBounds(tX, tY)) break;

                if (isOccupied(tX, tY)) {
                    if (isOccupiedBy(tX, tY, !chessman.whitesChessman)) {   // occupied by enemy
                        result.put(tX + (tY * 8), MoveType.ATTACK);
                    }

                    break;
                } else {
                    result.put(tX + (tY * 8), MoveType.NORMAL);
                }
            }

            tX = x;
            tY = y;
            while (true) {
                --tX;
                ++tY;

                if (isOutOfBounds(tX, tY)) break;

                if (isOccupied(tX, tY)) {
                    if (isOccupiedBy(tX, tY, !chessman.whitesChessman)) {   // occupied by enemy
                        result.put(tX + (tY * 8), MoveType.ATTACK);
                    }

                    break;
                } else {
                    result.put(tX + (tY * 8), MoveType.NORMAL);
                }
            }

            tX = x;
            tY = y;
            while (true) {
                ++tX;
                --tY;

                if (isOutOfBounds(tX, tY)) break;

                if (isOccupied(tX, tY)) {
                    if (isOccupiedBy(tX, tY, !chessman.whitesChessman)) {   // occupied by enemy
                        result.put(tX + (tY * 8), MoveType.ATTACK);
                    }

                    break;
                } else {
                    result.put(tX + (tY * 8), MoveType.NORMAL);
                }
            }

            tX = x;
            tY = y;
            while (true) {
                --tX;
                --tY;

                if (isOutOfBounds(tX, tY)) break;

                if (isOccupied(tX, tY)) {
                    if (isOccupiedBy(tX, tY, !chessman.whitesChessman)) {   // occupied by enemy
                        result.put(tX + (tY * 8), MoveType.ATTACK);
                    }

                    break;
                } else {
                    result.put(tX + (tY * 8), MoveType.NORMAL);
                }
            }
        };

        Runnable rook = () -> {
            int tX = x;
            int tY = y;

            boolean mode = true;
            while (true) {
                if (!mode && tX > x) tX = x;
                tX += mode ? 1 : -1;

                if (!isOutOfBounds(tX, tY)) {
                    if (!isOccupied(tX, tY)) {
                        result.put(tX + (tY * 8), MoveType.NORMAL);
                    } else {
                        if (isOccupiedBy(tX + (tY * 8), !chessman.whitesChessman)) {   // occupied by enemy
                            result.put(tX + (tY * 8), MoveType.ATTACK);
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
                        result.put(tX + (tY * 8), MoveType.NORMAL);
                    } else {
                        if (isOccupiedBy(tX + (tY * 8), !chessman.whitesChessman)) {   // occupied by enemy
                            result.put(tX + (tY * 8), MoveType.ATTACK);
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
                int field = index + (chessman.whitesChessman ? -8 : 8);
                if (!isOutOfBounds(field) && !isOccupied(field)) {
                    result.put(field, MoveType.NORMAL);

                    if (!chessman.hasMovedAtLeasOnce()) {
                        field = index + (chessman.whitesChessman ? -16 : 16);
                        if (!isOccupied(field)) {
                            result.put(field, MoveType.NORMAL);
                        }
                    }
                }

                field = index + (chessman.whitesChessman ? -7 : 7);
                if (!isOutOfBounds(field) && isOccupiedBy(field, !chessman.whitesChessman)) {
                    result.put(field, MoveType.ATTACK);
                }

                field = index + (chessman.whitesChessman ? -9 : 9);
                if (!isOutOfBounds(field) && isOccupiedBy(field, !chessman.whitesChessman)) {
                    result.put(field, MoveType.ATTACK);
                }
                field = index + (chessman.whitesChessman ? -7 : 9);
                if (!isOutOfBounds(field) && !isOccupied(field)) {
                    if (isOccupiedBy(index + 1, !chessman.whitesChessman) && getCachedChessman(index + 1).hasDoublePawnMove()) {
                        result.put(field, MoveType.EN_PASSANT);
                    }
                }
                field = index + (chessman.whitesChessman ? -9 : 7);
                if (!isOutOfBounds(field) && !isOccupied(field)) {
                    if (isOccupiedBy(index - 1, !chessman.whitesChessman) && getCachedChessman(index - 1).hasDoublePawnMove()) {
                        result.put(field, MoveType.EN_PASSANT);
                    }
                }

                break;
            case KNIGHT:
                int tX = x + 1;
                int tY = y + 2;

                // TODO: cleanup

                if (!isOutOfBounds(tX, tY) && !isOccupiedBy(tX, tY, chessman.whitesChessman)) {
                    result.put(tX + (tY * 8), MoveType.NORMAL);
                }

                tX = x - 1;
                if (!isOutOfBounds(tX, tY) && !isOccupiedBy(tX, tY, chessman.whitesChessman)) {
                    result.put(tX + (tY * 8), MoveType.NORMAL);
                }

                tY = y - 2;
                if (!isOutOfBounds(tX, tY) && !isOccupiedBy(tX, tY, chessman.whitesChessman)) {
                    result.put((x - 1) + (tY * 8), MoveType.NORMAL);
                }

                tX = x + 1;
                if (!isOutOfBounds(tX, tY) && !isOccupiedBy(tX, tY, chessman.whitesChessman)) {
                    result.put((x + 1) + (tY * 8), MoveType.NORMAL);
                }

                tX = x + 2;
                tY = y + 1;
                if (!isOutOfBounds(tX, tY) && !isOccupiedBy(tX, tY, chessman.whitesChessman)) {
                    result.put(tX + (tY * 8), MoveType.NORMAL);
                }

                tX = x - 2;
                if (!isOutOfBounds(tX, tY) && !isOccupiedBy(tX, tY, chessman.whitesChessman)) {
                    result.put(tX + (tY * 8), MoveType.NORMAL);
                }

                tY = y - 1;
                tX = x + 2;
                if (!isOutOfBounds(tX, tY) && !isOccupiedBy(tX, tY, chessman.whitesChessman)) {
                    result.put(tX + (tY * 8), MoveType.NORMAL);
                }

                tX = x - 2;
                if (!isOutOfBounds(tX, tY) && !isOccupiedBy(tX, tY, chessman.whitesChessman)) {
                    result.put(tX + (tY * 8), MoveType.NORMAL);
                }
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
                int[] toCheck = new int[] {
                        index + 1, index - 1,   // left, right
                        index + 8, index - 8,   // above, below
                        index + 7, index - 7,   // diagonally
                        index + 9, index - 9};  // diagonally

                // TODO: Check if moving to close to enemy king
                if (!chessman.hasMovedAtLeasOnce() && !isOccupied(index + 1) && !isOccupied(index + 1) && !getCachedChessman(index + 3).hasMovedAtLeasOnce()) {
                    result.put(index + 2, MoveType.CASTLING);
                }
                if (!chessman.hasMovedAtLeasOnce() && !isOccupied(index - 1) && !isOccupied(index - 2) && !isOccupied(index - 3) && !getCachedChessman(index - 4).hasMovedAtLeasOnce()) {
                    result.put(index - 3, MoveType.CASTLING);
                }

                for (int i : toCheck) {
                    if (!isOutOfBounds(i)) {
                        if (isOccupied(i)) {
                            if (isOccupiedBy(i, !chessman.whitesChessman)) {   // occupied by enemy
                                result.put(i, MoveType.ATTACK);
                            }
                        } else {
                            result.put(i, MoveType.NORMAL);
                        }
                    }
                }
                break;
            default:
                throw new RuntimeException("Not implemented");
        }

        return result;
    }

    public boolean isValidMove(int field, int targetField) {
        if (field == targetField) {
            throw new IllegalArgumentException("Tried to move a chessman to the location it's currently on");
        } else if (getCachedChessman(targetField) != null && getCachedChessman(targetField).type == Chessman.KING) {
            throw new IllegalArgumentException("Tried to move a chessman onto an king");
        }

        return getPossibleMoves(field).containsKey(targetField);
    }

    ActiveChessman getCachedChessman(int field) {
        return this.cachedBoard[field];
    }

    private boolean isOccupied(int field) {
        return this.cachedBoard[field] != null;
    }

    private boolean isOccupied(int x, int y) {
        return isOccupied(x + (y * 8));
    }

    private boolean isOccupiedBy(int field, boolean whiteChessman) {
        ActiveChessman chessman = this.cachedBoard[field];

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

//    public void printCachedBoard() {
//        System.out.print(' ');
//        for (int i = 0; i < cachedBoard.length + 2; i++) {
//            System.out.print('─');
//        }
//        System.out.println();
//
//                ActiveChessman chessman = activeChessmen[j];
//
//                if (chessman == null) {
//                    System.out.print(' ');
//                } else {
//                    System.out.print(chessman.whitesChessman ? chessman.type.charWhite : chessman.type.charBlack);
//                }
//
//        System.out.print(' ');
//        for (int i = 0; i < cachedBoard.length + 2; i++) {
//            System.out.print('─');
//        }
//        System.out.println();
//    }
}