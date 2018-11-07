import java.awt.*;

public enum Colour {
    RED(Color.RED), GREEN(Color.GREEN), BLUE(Color.BLUE), YELLOW(Color.YELLOW);

    private final Color color;

    Colour(Color color) {
        this.color = color;
    }

    Color getAWTColor() {
        return color;
    }
}
