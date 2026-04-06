package com.boaat.jazzy_cookin.screen.layout;

public record LayoutRegion(int x, int y, int width, int height) {
    public int right() {
        return this.x + this.width;
    }

    public int bottom() {
        return this.y + this.height;
    }

    public int centerX() {
        return this.x + this.width / 2;
    }

    public int centerY() {
        return this.y + this.height / 2;
    }

    public boolean intersects(LayoutRegion other) {
        return other != null
                && this.x < other.right()
                && this.right() > other.x
                && this.y < other.bottom()
                && this.bottom() > other.y;
    }

    public boolean contains(LayoutRegion other) {
        return other != null
                && other.x >= this.x
                && other.right() <= this.right()
                && other.y >= this.y
                && other.bottom() <= this.bottom();
    }

    public boolean contains(int pointX, int pointY) {
        return pointX >= this.x && pointX < this.right() && pointY >= this.y && pointY < this.bottom();
    }
}
