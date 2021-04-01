package de.sprax2013.hems.analog_chess;

import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class ChessGui {
    private static final Color WHITE_FIELD = new Color(0xE3, 0xC1, 0x6F);
    private static final Color BLACK_FIELD = new Color(0xB8, 0x8B, 0x4A);

    private static final Border LOWERED_BORDER = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
    private static final Border PADDING_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);

    private static final Border DEBUG_BORDER = BorderFactory.createCompoundBorder(PADDING_BORDER,
            BorderFactory.createLineBorder(Color.WHITE, 2, false));

    private static final Border NORMAL_MOVE_BORDER = BorderFactory.createCompoundBorder(PADDING_BORDER,
            BorderFactory.createLineBorder(Color.GREEN, 2, false));
    private static final Border ATTACK_MOVE_BORDER = BorderFactory.createCompoundBorder(PADDING_BORDER,
            BorderFactory.createLineBorder(Color.RED, 2, false));
    private static final Border KING_IN_CHECK_BORDER = BorderFactory.createCompoundBorder(PADDING_BORDER,
            BorderFactory.createLineBorder(Color.MAGENTA, 4, false));

    private static final Border SPECIAL_MOVE_BORDER = BorderFactory.createCompoundBorder(PADDING_BORDER,
            BorderFactory.createLineBorder(Color.YELLOW, 2, false));
    private static final Border PROMOTION_MOVE_BORDER = BorderFactory.createCompoundBorder(PADDING_BORDER,
            BorderFactory.createLineBorder(Color.PINK, 2, false));
    private static final Border UNDER_PROMOTION_MOVE_BORDER = BorderFactory.createCompoundBorder(PADDING_BORDER,
            BorderFactory.createLineBorder(Color.CYAN, 2, false));

    private final ChessGame game;

    private JFrame gui;
    private final JPanel[] fieldPanels;

    private int activeField = -1;
    private @NotNull Map<Integer, MoveType> possibleMoves = Collections.emptyMap();

    public ChessGui(ChessGame game) {
        this.game = game;
        this.fieldPanels = new JPanel[ChessGame.BOARD_SIZE];

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Schach");

            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setResizable(false); // TODO: Correctly resize window
            frame.setSize(75 * 8, 75 * 8);
            frame.getContentPane().setLayout(new GridLayout(8, 8));

            try {
                frame.setIconImage(Chessman.KING.getBlackImage());
            } catch (IOException ex) {
                System.err.println("Error setting app icon: " + ex.getMessage());
            }

            boolean b = false;
            for (int i = 0; i < this.fieldPanels.length; ++i) {
                if (i % 8 == 0) b = !b;

                JPanel panel = new JPanel();
                this.fieldPanels[i] = panel;
                frame.getContentPane().add(panel);

                panel.setBackground(b ? WHITE_FIELD : BLACK_FIELD);

                JLabel label = new JLabel();
                panel.add(label);

                int currPanelIndex = i;
                panel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);

                        if (possibleMoves.containsKey(currPanelIndex)) {
                            try {
                                game.moveChessman(activeField, currPanelIndex);

                                setActiveField(null);
                                update();
                            } catch (IllegalArgumentException ex) {
                                System.err.println(ex.getMessage());
                            }
                        } else {
                            if (activeField != currPanelIndex) {
                                setActiveField(currPanelIndex);
                            } else {
                                setActiveField(null);
                            }

                            updateFieldBorders();
                        }
                    }
                });

                b = !b;
            }

            this.gui = frame;

            update();
            frame.setVisible(true);
        });
    }

    public void setHighlightedFields(boolean[] fields) {
        if (fields.length != 64) throw new IllegalArgumentException("Array is not 64 large");

        SwingUtilities.invokeLater(() -> {
            synchronized (this.gui.getTreeLock()) {
                for (int i = 0; i < fieldPanels.length; ++i) {
                    this.fieldPanels[i].setBorder(fields[i] ? DEBUG_BORDER : null);
                }
            }
        });
    }

    private void updateFieldBorders() {
        if (this.gui == null) return;

        SwingUtilities.invokeLater(() -> {
            synchronized (this.gui.getTreeLock()) {
                for (int i = 0; i < fieldPanels.length; ++i) {
                    JPanel panel = this.fieldPanels[i];

                    /* Border */
                    Border border;

                    if (this.activeField == i) {
                        border = LOWERED_BORDER;
                    } else {
                        MoveType moveType = this.possibleMoves.get(i);

                        if (moveType != null) {
                            switch (moveType) {
                                case NORMAL:
                                case PAWN_DOUBLE_MOVE:
                                    border = NORMAL_MOVE_BORDER;
                                    break;
                                case ATTACK:
                                    border = ATTACK_MOVE_BORDER;
                                    break;
                                case PROMOTION:
                                    border = PROMOTION_MOVE_BORDER;
                                    break;
                                case UNDER_PROMOTION:
                                    border = UNDER_PROMOTION_MOVE_BORDER;
                                    break;
                                default:
                                    border = SPECIAL_MOVE_BORDER;
                                    break;
                            }
                        } else {
                            border = null;
                        }
                    }

                    panel.setBorder(border);
                }

                int kingInCheckIndex = this.game.getKingInCheck();

                if (kingInCheckIndex != -1) {
                    this.fieldPanels[kingInCheckIndex].setBorder(KING_IN_CHECK_BORDER);
                }
            }
        });
    }

    private void updateChessmen() {
        if (this.gui == null) return;

        SwingUtilities.invokeLater(() -> {
            synchronized (this.gui.getTreeLock()) {
                for (int i = 0; i < fieldPanels.length; ++i) {
                    JPanel panel = this.fieldPanels[i];
                    JLabel label = (JLabel) panel.getComponent(0);
                    ActiveChessman chessman = this.game.getChessmanAt(i);

                    if (chessman == null) {
                        label.setIcon(null);
                    } else {
                        try {
                            label.setIcon(new ImageIcon(chessman.whitesChessman ? chessman.type.getWhiteImage() : chessman.type.getBlackImage()));
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(this.gui, "Error updating chessman (" + chessman.type + ") image: " + ex.getMessage(), "An error occured", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });
    }

    private void update() {
        updateChessmen();
        updateFieldBorders();
    }

    private void setActiveField(Integer fieldIndex) {
        if (fieldIndex == null ||
                this.game.getChessmanAt(fieldIndex) == null ||
                (!ChessGame.DEBUG_IGNORE_TURNS && this.game.getChessmanAt(fieldIndex).whitesChessman != this.game.isWhitesTurn())) {
            this.activeField = -1;
            this.possibleMoves = Collections.emptyMap();
        } else {
            this.activeField = fieldIndex;
            this.possibleMoves = this.game.getPossibleMoves(this.activeField);
        }
    }
}