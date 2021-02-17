package de.sprax2013.hems.analog_chess;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Locale;

public enum Chessman {
    PAWN('♙', '♟', 'B'),
    KNIGHT('♘', '♞', 'S'),
    BISHOP('♗', '♝', 'L'),
    ROOK('♖', '♜', 'T'),
    QUEEN('♕', '♛', 'D'),
    KING('♔', '♚', 'K');

    public final char charWhite;
    public final char charBlack;

    public final char notationDE;

    private BufferedImage imgWhite;
    private BufferedImage imgBlack;

    Chessman(char charWhite, char charBlack, char notationDE) {
        this.charWhite = charWhite;
        this.charBlack = charBlack;

        this.notationDE = notationDE;
    }

    public BufferedImage getWhiteImage() throws IOException {
        if (this.imgWhite == null) {
            this.imgWhite = ImageIO.read(Chessman.class.getResourceAsStream("/chessman/white/" + this.name().toLowerCase(Locale.ROOT) + ".png"));
        }

        return this.imgWhite;
    }

    public BufferedImage getBlackImage() throws IOException {
        if (this.imgBlack == null) {
            this.imgBlack = ImageIO.read(Chessman.class.getResourceAsStream("/chessman/black/" + this.name().toLowerCase(Locale.ROOT) + ".png"));
        }

        return this.imgBlack;
    }
}