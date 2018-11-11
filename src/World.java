import jade.lang.acl.ACLMessage;
import jade.wrapper.StaleProxyException;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.space.Object2DTorus;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class World extends Agent {

    private Object2DTorus space;
    private ArrayList<Person> agentsList;
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
    private Integer tickDelay;
    private Random random;
    private Integer sentMsgNo = 0;
    private WorldState state = WorldState.IMMIGRATION;
    private Integer receivedMsgNo = 0;
    private OpenSequenceGraph plot;
    private boolean smartChoice;

    public World(ArrayList<Person> agentsList,
                 Object2DTorus space,
                 Random random,
                 Integer length,
                 Integer width,
                 Double initialPtr,
                 Double deathRate,
                 Double mutationRate,
                 Integer immigrantsPerDay,
                 Double immigrantChanceCooperateWithSame,
                 Double immigrantChanceCooperateWithDifferent,
                 int tickDelay,
                 boolean smartChoice,
                 OpenSequenceGraph plot) {
        this.tickDelay = tickDelay;
        this.agentsList = agentsList;
        this.space = space;
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
        this.plot = plot;
        this.smartChoice = smartChoice;
    }

    Person getPerson(Point point) {
        return (Person) space.getObjectAt((int) point.getX(), (int) point.getY());
    }

    void killPerson(Person person) {
        space.putObjectAt(person.getX(), person.getY(), null);
        person.doDelete();
    }

    public void putPerson(Person person) throws StaleProxyException {
        this.getContainerController().acceptNewAgent("person-" + person.getId(), person).start();
        space.putObjectAt(person.getX(),person.getY(), person);
    }

    public ArrayList<Person> getNeighbours(Point point) {
        ArrayList<Person> neighbours = new ArrayList<>();

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

        int x = random.nextInt(length);
        int y = random.nextInt(width);
        Point point = new Point(x, y);
        while (getPerson(point) != null) {
            x = random.nextInt(length);
            y = random.nextInt(width);
            point = new Point(x, y);
        }

        immigrant.setLocation(point);
        try {
            putPerson(immigrant);
            agentsList.add(immigrant);

        } catch (Exception e) {
            e.printStackTrace();
        }

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

        return new Person(initialPtr, colour, cooperateWithSame, cooperateWithDifferent, null, smartChoice);
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
        doImmigration();
        doInteraction();
        doReproduction();
        doCulling();
    }

    private void doCulling() {
        // Stage 4: Die :-(
        ArrayList<Person> tempDeath = new ArrayList<>();
        for (Person person : agentsList) {
            if (random.nextDouble() < deathRate) {
                killPerson(person); // rest in peace sweet prince
                tempDeath.add(person);
            }
        }
        for (Person p : tempDeath) {
            agentsList.remove(p);
        }
    }

    private void doReproduction() {
        // Stage 3: Reproduce
        Collections.shuffle(agentsList, random);
        ArrayList<Person> tempReproduce = new ArrayList<>();
        for (int i = 0; i < agentsList.size(); i++) {
            Person person = agentsList.get(i);
            ArrayList<Point> emptySites = getEmptyAdjacentSites(person.getLocation());
            if (emptySites.isEmpty()) {
                continue;
            }
            Person child = person.reproduce(mutationRate, random);
            if (child != null) {
                Point location = emptySites.iterator().next();
                child.setPtr(initialPtr);
                child.setLocation(location);
                tempReproduce.add(child);
                try {
                    putPerson(child);
                    agentsList.add(i + 1, child);
                    i++;
                } catch (StaleProxyException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doInteraction() {
        // Stage 2: Help others (or not)
        sentMsgNo = 0;
        Collections.shuffle(agentsList, random);
        for (Person person : agentsList) {
            ArrayList<Person> neighbours = getNeighbours(person.getLocation());
            if (neighbours.isEmpty()) {
                continue;
            }
            this.sendMessageToPerson(person, neighbours);
            sentMsgNo++;
        }
    }

    private void doImmigration() {
        // Stage 1: place immigrants
        for (int i = 0; i < immigrantsPerDay; i++) {
            putImmigrant();
        }
    }

    private void sendMessageToPerson(Person person, ArrayList<Person> neighbours) {

        ACLMessage message = new ACLMessage(ACLMessage.INFORM);

        message.addReceiver(person.getAID());

        StringBuilder content = new StringBuilder();
        for (Person neighbour : neighbours) {
            content.append(neighbour.getName()).append(",");
        }
        message.setContent(content.toString());

        message.addReplyTo(getAID());

        this.send(message);


    }

    private ArrayList<Point> getNeighboursPositions(Point location) {
        ArrayList<Point> busySites = new ArrayList<>();

        int x = (int) location.getX();
        int y = (int) location.getY();

        Point up = new Point(x, y - 1);
        Point down = new Point(x, y + 1);
        Point left = new Point(x - 1, y);
        Point right = new Point(x + 1, y);

        if (getPerson(up) != null) {
            busySites.add(up);
        }
        if (getPerson(down) != null) {
            busySites.add(down);
        }
        if (getPerson(left) != null) {
            busySites.add(left);
        }
        if (getPerson(right) != null) {
            busySites.add(right);
        }

        Collections.shuffle(busySites, random);

        return busySites;
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
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                switch (state){
                    case IMMIGRATION:
                        doImmigration();
                        state = WorldState.INTERACTION;
                        break;
                    case INTERACTION:
                        doInteraction();
                        state = WorldState.WAITING;
                        break;
                    case WAITING:
                        //System.out.println("Received msg no: " + receivedMsgNo);
                        //System.out.println("Sent msg no: " + sentMsgNo);
                        receiveMessages();
                        if (receivedMsgNo.equals(sentMsgNo)) {
                            state = WorldState.REPRODUCTION;
                        }
                        break;
                    case REPRODUCTION:
                        doReproduction();
                        state = WorldState.CULLING;
                        break;
                    case CULLING:
                        doCulling();
                        receivedMsgNo = 0;
                        sentMsgNo = 0;
                        state = WorldState.IMMIGRATION;
                        plot.step();
                        break;
                    default:
                        // do nothing
                }
            }
        });
        System.out.println(getLocalName() + ": starting to work!");
    }

    public boolean isSmartChoice() {
        return smartChoice;
    }

    public void setSmartChoice(boolean smartChoice) {
        this.smartChoice = smartChoice;
        for (Person p : agentsList) {
            p.setSmartChoice(smartChoice);
        }
    }

    private void receiveMessages() {
        while (receive() != null) {
            receivedMsgNo++;
            //System.out.println("World Received " + msg.getContent());
        }
    }

    public void takeDown() {
        System.out.println("This " + getLocalName() + " has ended");
    }

    public void setInitialPtr(Double initialPtr) {
        this.initialPtr = initialPtr;
    }

    public void setImmigrantsPerDay(Integer immigrantsPerDay) {
        this.immigrantsPerDay = immigrantsPerDay;
    }

    public void setImmigrantChanceCooperateWithSame(Double immigrantChanceCooperateWithSame) {
        this.immigrantChanceCooperateWithSame = immigrantChanceCooperateWithSame;
    }

    public void setImmigrantChanceCooperateWithDifferent(Double immigrantChanceCooperateWithDifferent) {
        this.immigrantChanceCooperateWithDifferent = immigrantChanceCooperateWithDifferent;
    }

    public void setCostOfGiving(Double costOfGiving) {
        this.costOfGiving = costOfGiving;
    }

    public void setGainOfReceiving(Double gainOfReceiving) {
        this.gainOfReceiving = gainOfReceiving;
    }

    public void setMutationRate(Double mutationRate) {
        this.mutationRate = mutationRate;
    }

    public void setDeathRate(Double deathRate) {
        this.deathRate = deathRate;
    }

    public Integer getTickDelay() {
        return tickDelay;
    }

    public void setTickDelay(Integer tickDelay) {
        this.tickDelay = tickDelay;
    }
}