import jade.core.Agent;

import java.util.HashSet;
import java.util.Random;

public class World extends Agent {

    private Person[][] terrain;
    private Integer length;
    private Integer width;
    private Double initialPtr;
    private Integer immigrantsPerDay;
    private Double immigrantChanceCooperateWithSame;
    private Double immigrantChanceCooperateWithDifferent;
    private Random random;

    public World(Random random) {
        this(random, 50, 50);
    }

    public World(Random random, Integer length, Integer width) {
        this(random, length, width, 0.12, 1, 0.5, 0.5);
    }

    public World(Random random, Integer length, Integer width, Double initialPtr, Integer immigrantsPerDay, Double immigrantChanceCooperateWithSame, Double immigrantChanceCooperateWithDifferent) {
        this.random = random;
        this.length = length;
        this.width = width;
        terrain = new Person[this.width][this.length];
        this.initialPtr = initialPtr;
        this.immigrantsPerDay = immigrantsPerDay;
        this.immigrantChanceCooperateWithSame = immigrantChanceCooperateWithSame;
        this.immigrantChanceCooperateWithDifferent = immigrantChanceCooperateWithDifferent;
    }

    Person getPerson(Integer x, Integer y) {
        return terrain[y][x];
    }

    HashSet<Person> getNeighbours(Integer x, Integer y) {
        HashSet<Person> neighbours = new HashSet<>();

        if (terrain[y % width][(x + 1) % length] != null) {
            neighbours.add(terrain[y % width][(x + 1) % length]);
        } else if (terrain[y % width][(x - 1) % length] != null) {
            neighbours.add(terrain[y % width][(x - 1) % length]);
        } else if (terrain[(y + 1) % width][x % length] != null) {
            neighbours.add(terrain[(y + 1) % width][x % length]);
        } else if (terrain[(y - 1) % width][x % length] != null) {
            neighbours.add(terrain[(y - 1) % width][x % length]);
        }

        return neighbours;
    }

    void putImmigrant() {

    }

}
