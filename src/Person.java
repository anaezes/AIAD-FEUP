import jade.core.Agent;

import java.util.Random;

public class Person extends Agent {
    private Double ptr;
    private Colour colour;
    private Boolean cooperateWithSame;
    private Boolean cooperateWithDifferent;

    public Person(Double ptr, Colour colour, Boolean cooperateWithSame, Boolean cooperateWithDifferent) {
        this.ptr = ptr;
        this.colour = colour;
        this.cooperateWithSame = cooperateWithSame;
        this.cooperateWithDifferent = cooperateWithDifferent;
    }

    Person reproduce(Double mutationRate) {
        new Random().nextDouble();
    }
}
