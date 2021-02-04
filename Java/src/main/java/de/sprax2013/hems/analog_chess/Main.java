package de.sprax2013.hems.analog_chess;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class Main {
    private static final Color WHITE_FIELD = new Color(0xE3, 0xC1, 0x6F);
    private static final Color BLACK_FIELD = new Color(0xB8, 0x8B, 0x4A);

    private static final JLabel[] guiChessFields = new JLabel[8 * 8];

    static JFrame gui;
    private static ChessGame game;

    private static Map<Integer, MoveType> highlightedFields = Collections.emptyMap();

    public static void main(String[] args) {
        game = new ChessGame();

        SwingUtilities.invokeLater(Main::gui);
    }

    static void gui() {
        gui = new JFrame("Schachbrett");
        gui.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        gui.setLayout(new GridLayout(8, 8));
        gui.setSize(125 * 8, 125 * 8);

        boolean b = false;
        for (int i = 0; i < 8 * 8; ++i) {
            if (i % 8 == 0) b = !b;

            JPanel panel = new JPanel();
            panel.setBackground(b ? WHITE_FIELD : BLACK_FIELD);

            JLabel label = new JLabel(Integer.toString(i));

            guiChessFields[i] = label;
            panel.add(label);

            gui.add(panel);

            int finalI = i;
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    ActiveChessman chessman = game.getCachedChessman(finalI);

                    if (chessman != null && chessman.whitesChessman == game.isWhitesTurn()) {
                        highlightedFields = game.getPossibleMoves(finalI);
                    } else {
                        highlightedFields = Collections.emptyMap();
                    }

                    updateGui();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    int w = gui.getWidth();
                    int h = gui.getHeight();

                    int x = panel.getX() + e.getX();
                    int y = panel.getY() + e.getY();

                    int fW = w / 8;
                    int fH = h / 8;

                    int xI = x / fW;
                    int yI = y / fH;

                    int targetI = xI + (yI * 8);

                    if (finalI != targetI) {
                        ActiveChessman chessman = game.getCachedChessman(finalI);

                        if (chessman != null && chessman.whitesChessman == game.isWhitesTurn()) {
                            game.moveChessman(game.isWhitesTurn(), finalI, targetI);

                            highlightedFields = Collections.emptyMap();
                            updateGui();
                        }
                    }
                }
            });

            b = !b;
        }

        gui.setVisible(true);

        updateGui();
    }

    static void updateGui() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 8 * 8; i++) {
                ActiveChessman chessman = game.getCachedChessman(i);

                JLabel label = guiChessFields[i];

                MoveType highlightedMove = highlightedFields.get(i);

                if (label.getText().endsWith("-Possible")) {
                    label.setText(label.getText().substring(0, label.getText().indexOf('-')));
                }

                if (highlightedMove != null) {
                    label.setText(label.getText() + "-Possible");
                }

                if (chessman == null) {
                    label.setIcon(null);
                } else {
                    try {
                        label.setIcon(new ImageIcon(chessman.whitesChessman ? chessman.type.getWhiteImage() : chessman.type.getBlackImage()));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }
}