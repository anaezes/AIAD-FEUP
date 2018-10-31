import jade.core.Agent;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import java.awt.*;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class Person extends Agent implements Drawable {
    private Double ptr;
    private Colour colour;
    private Boolean cooperateWithSame;
    private Boolean cooperateWithDifferent;
    private Point location;
    private UUID id;

    public Person(Double ptr, Colour colour, Boolean cooperateWithSame, Boolean cooperateWithDifferent, Point location) {
        this.ptr = ptr;
        this.colour = colour;
        this.cooperateWithSame = cooperateWithSame;
        this.cooperateWithDifferent = cooperateWithDifferent;
        this.location = location;
        this.id = UUID.randomUUID();
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(ptr, person.ptr) &&
                colour == person.colour &&
                Objects.equals(cooperateWithSame, person.cooperateWithSame) &&
                Objects.equals(cooperateWithDifferent, person.cooperateWithDifferent) &&
                Objects.equals(id, person.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ptr, colour, cooperateWithSame, cooperateWithDifferent, id);
    }

    public void setPtr(Double ptr) {
        this.ptr = ptr;
    }

    Person reproduce(Double mutationRate, Random random) {
        if (random.nextDouble() >= ptr) {
            return null;
        }

        boolean cooperateWithSame = this.cooperateWithSame;
        boolean cooperateWithDifferent = this.cooperateWithDifferent;
        Colour colour = this.colour;

        // mutate colour
        if (random.nextDouble() < mutationRate) {
            double colourValue = random.nextDouble();

            if (colourValue < 0.25) {
                colour = Colour.RED;
            } else if (colourValue < 0.5) {
                colour = Colour.GREEN;
            } else if (colourValue < 0.75) {
                colour = Colour.BLUE;
            } else {
                colour = Colour.YELLOW;
            }
        }

        // mutate cooperation with same
        if (random.nextDouble() < mutationRate) {
            cooperateWithSame = !cooperateWithSame;
        }

        // mutate cooperation with different
        if (random.nextDouble() < mutationRate) {
            cooperateWithDifferent = !cooperateWithDifferent;
        }

        return new Person(null, colour, cooperateWithSame, cooperateWithDifferent, null);
    }

    @Override
    public String toString() {
        return "Person{" +
                ", colour=" + colour +
                ", coopSame=" + cooperateWithSame +
                ", coopDiff=" + cooperateWithDifferent +
                ", location=" + location +
                '}';
    }

    @Override
    public void draw(SimGraphics simGraphics) {
        simGraphics.drawFastCircle(colour.getAWTColor());
    }

    @Override
    public int getX() {
        return (int)location.getX();
    }

    @Override
    public int getY() {
        return (int)location.getY();
    }
}
