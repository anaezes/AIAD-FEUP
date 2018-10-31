import jade.core.Agent;
import jade.core.behaviours.Behaviour;

import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class World extends Agent {

    private ConcurrentHashMap<Point, Person> terrain;
    private Integer length;
    private Integer width;
    private Double initialPtr;
    private Integer immigrantsPerDay;
    private Double immigrantChanceCooperateWithSame;
    private Double immigrantChanceCooperateWithDifferent;
    private Double costOfGiving;
    private Double gainOfReceiving;
    private Double mutationRate;
    private Double deathRate;
    private Integer tick;
    private Random random;

    public World(Random random) {
        this(random, 50, 50);
    }

    public World(Random random, Integer length, Integer width) {
        this(random, length, width, 0.12, 0.1, 0.05, 1, 0.5, 0.5);
    }

    public World(Random random,
                 Integer length,
                 Integer width,
                 Double initialPtr,
                 Double deathRate,
                 Double mutationRate,
                 Integer immigrantsPerDay,
                 Double immigrantChanceCooperateWithSame,
                 Double immigrantChanceCooperateWithDifferent) {
        this.random = random;
        this.length = length;
        this.width = width;
        this.terrain = new ConcurrentHashMap<>();
        this.initialPtr = initialPtr;
        this.deathRate = deathRate;
        this.mutationRate = mutationRate;
        this.immigrantsPerDay = immigrantsPerDay;
        this.immigrantChanceCooperateWithSame = immigrantChanceCooperateWithSame;
        this.immigrantChanceCooperateWithDifferent = immigrantChanceCooperateWithDifferent;
    }

    Person getPerson(Point point) {
        return terrain.get(this.normalize(point));
    }

    public void putPerson(Point point, Person person) {
        terrain.put(this.normalize(point), person);
    }

    public HashSet<Person> getNeighbours(Point point) {
        HashSet<Person> neighbours = new HashSet<>();

        int x = (int) point.getX();
        int y = (int) point.getY();

        Point up = new Point(x, y - 1);
        Point down = new Point(x, y + 1);
        Point left = new Point(x - 1, y);
        Point right = new Point(x + 1, y);

        if (getPerson(up) != null) {
            neighbours.add(getPerson(up));
        }
        if (getPerson(down) != null) {
            neighbours.add(getPerson(down));
        }
        if (getPerson(left) != null) {
            neighbours.add(getPerson(left));
        }
        if (getPerson(right) != null) {
            neighbours.add(getPerson(right));
        }

        return neighbours;
    }

    public void putImmigrant() {
        Person immigrant = randomPerson();

        Integer x = random.nextInt(length);
        Integer y = random.nextInt(width);
        Point point = new Point(x, y);
        while (getPerson(point) != null) {
            x = random.nextInt(length);
            y = random.nextInt(width);
            point = new Point(x, y);
        }

        immigrant.setLocation(new Point(x, y));
        putPerson(point, immigrant);
    }

    public Person randomPerson() {
        boolean cooperateWithSame = false;
        boolean cooperateWithDifferent = false;
        Colour colour;

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

        if (random.nextDouble() < immigrantChanceCooperateWithSame) {
            cooperateWithSame = true;
        }
        if (random.nextDouble() < immigrantChanceCooperateWithDifferent) {
            cooperateWithDifferent = true;
        }

        return new Person(initialPtr, colour, cooperateWithSame, cooperateWithDifferent, null);
    }

    public ArrayList<Person> getRandomPopulation() {
        ArrayList<Person> population = new ArrayList<>(terrain.values());
        Collections.shuffle(population, random);
        return population;
    }

    public Point normalize(Point point) {
        int x = (int) point.getX() % this.length;
        int y = (int) point.getY() % this.width;
        return new Point(x, y);
    }

    public void tick() {
        // Stage 1: place immigrants
        for (int i = 0; i < immigrantsPerDay; i++) {
            putImmigrant();
        }

        // Stage 2: Help others (or not)


        // Stage 3: Reproduce
        for (Person person : getRandomPopulation()) {
            ArrayList<Point> emptySites = getEmptyAdjacentSites(person.getLocation());
            if (emptySites.isEmpty()) {
                continue;
            }
            Person child = person.reproduce(mutationRate, random);
            if (child != null) {
                Point location = emptySites.iterator().next();
                child.setPtr(initialPtr);
                child.setLocation(location);
                putPerson(location, child);
            }
        }

        // Stage 4: Die :-(
        for (Person person : terrain.values()) {
            if (random.nextDouble() < deathRate) {
                terrain.remove(person.getLocation()); // rest in peace sweet prince
            }
        }
    }

    public ArrayList<Point> getEmptyAdjacentSites(Point location) {
        ArrayList<Point> emptySites = new ArrayList<>();

        int x = (int) location.getX();
        int y = (int) location.getY();

        Point up = new Point(x, y - 1);
        Point down = new Point(x, y + 1);
        Point left = new Point(x - 1, y);
        Point right = new Point(x + 1, y);

        if (getPerson(up) == null) {
            emptySites.add(up);
        }
        if (getPerson(down) == null) {
            emptySites.add(down);
        }
        if (getPerson(left) == null) {
            emptySites.add(left);
        }
        if (getPerson(right) == null) {
            emptySites.add(right);
        }

        Collections.shuffle(emptySites, random);

        return emptySites;
    }

    public void setup() {
        addBehaviour(new WorkingBehaviour());

        System.out.println(getLocalName() + ": starting to work!");
    }

    public void takeDown() {
        System.out.println("This " + getLocalName() + " has ended");
    }

    class WorkingBehaviour extends Behaviour {
        private int n = 0;

        public void action() {
            System.out.println(++n + " I am doing something!");
            tick();
            System.out.println("This is the world state");
            for (Point person: terrain.keySet()){
                String key = person.toString();
                String value = terrain.get(person).toString();
                System.out.println(key + " " + value);
            }
        }

        public boolean done() {
            return n == 20;
        }
    }

}