import jade.core.Agent;

import java.util.*;

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
        Person immigrant = randomPerson();
        int y = random.nextInt(width);
        int x = random.nextInt(length);
        while (getPerson(x,y) != null) {
            y = random.nextInt(width);
            x = random.nextInt(length);
        }
        terrain[y][x] = immigrant;
    }

    private Person randomPerson() {
        boolean coopWithSame = false;
        boolean coopWithDiff = false;
        Colour colour = Colour.RED;
        double colourValue = random.nextDouble();

        if(colourValue >= 0.25 && colourValue < 0.5){
            colour = Colour.GREEN;
        } else if(colourValue <0.75){
            colour = Colour.BLUE;
        } else{
            colour = Colour.YELLOW;
        }

        if(random.nextDouble() > 0.5){
            coopWithSame = true;
        }
        if(random.nextDouble() > 0.5) {
            coopWithDiff = true;
        }
        return new Person(initialPtr,colour,coopWithSame,coopWithDiff);
    }

    private HashSet<Person> getAllPersons(){
        ArrayList<Person> population = new ArrayList<>();
        for (int i=0;i<terrain.length;i++){
            population.addAll(Arrays.asList(terrain[i]));
        }
        System.out.println("Before shuffle:" + population);
        Collections.shuffle(population,random);
        System.out.println("After shuffle:" + population);
    }
}
