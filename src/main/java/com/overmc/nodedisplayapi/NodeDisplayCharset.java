package com.overmc.nodedisplayapi;

public class NodeDisplayCharset {
    protected final char borderHorizontal;
    protected final char borderVertical;
    protected final char borderTopLeft;
    protected final char borderTopRight;
    protected final char borderBottomLeft;
    protected final char borderBottomRight;
    
    protected final char borderAltLeftFork; //The symbol that forks from the left of the border to the 'alt' charset.
    protected final char altHorizontalDown;
    protected final char altVerticalRight;
    protected final char altBottomLeftCorner;
    protected final char altHorizontal;
    protected final char altVertical;
    
    public NodeDisplayCharset(char borderHorizontal, char borderVertical, char borderTopLeft, char borderTopRight, char borderBottomLeft, char borderBottomRight, char borderAltLeftFork,
            char altHorizontalDown, char altVerticalRight, char altBottomLeftCorner, char altHorizontal, char altVertical) {
        this.borderHorizontal = borderHorizontal;
        this.borderVertical = borderVertical;
        this.borderTopLeft = borderTopLeft;
        this.borderTopRight = borderTopRight;
        this.borderBottomLeft = borderBottomLeft;
        this.borderBottomRight = borderBottomRight;
        this.borderAltLeftFork = borderAltLeftFork;
        this.altHorizontalDown = altHorizontalDown;
        this.altVerticalRight = altVerticalRight;
        this.altBottomLeftCorner = altBottomLeftCorner;
        this.altHorizontal = altHorizontal;
        this.altVertical = altVertical;
    }
}
