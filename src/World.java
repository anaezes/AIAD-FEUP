import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.wrapper.StaleProxyException;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.space.Object2DTorus;

import java.awt.*;
import java.util.*;
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
    private ContainerController mainContainer;
    private final AidHolder aidHolder = AidHolder.getInstance();

    public World(ArrayList<Person> agentsList, Object2DTorus space, Random random, ContainerController mainContainer) {
        this(agentsList, space, random, 50, 50, mainContainer);
    }

    public World(ArrayList<Person> agentsList,
                 Object2DTorus space,
                 Random random,
                 Integer length,
                 Integer width,
                 ContainerController mainContainer) {
        this(agentsList, space, random, length, width, 0.12, 0.1,
                0.05,
                1,
                0.5,
                0.5,
                0,
                mainContainer);
    }

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
                 ContainerController mainContainer) {
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
        this.mainContainer = mainContainer;
    }

    Person getPerson(Point point) {
        return (Person)space.getObjectAt((int)point.getX(),(int)point.getY());
    }

    void killPerson(Person person) {
        space.putObjectAt(person.getX(),person.getY(),null);
        person.doDelete();
        aidHolder.removeAID(person.getAID().getName());
    }

    public void putPerson(Person person) throws StaleProxyException {
        this.getContainerController().acceptNewAgent("person"+person.getId(),person).start();
        aidHolder.addAID(person.getAID());
        space.putObjectAt(person.getX(),person.getY(), person);
    }

    /*public HashSet<Person> getNeighbours(Point point) {
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
    }*/

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

        immigrant.setLocation(point);
        try {
            putPerson(immigrant);
            agentsList.add(immigrant);

        } catch(Exception e) {
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
        Collections.shuffle(agentsList, random);
        for(int i = 0; i < agentsList.size(); i++){
            Person person = agentsList.get(i);
            ArrayList<Point> neighborsSites = getNeighborsPositions(person.getLocation());
            if(neighborsSites.isEmpty()){
                continue;
            }
            this.sendMessageToPerson(neighborsSites, person);
        }

        // Stage 3: Reproduce
        Collections.shuffle(agentsList,random);
        ArrayList<Person> tempReproduce = new ArrayList<>();
        for (int i = 0; i < agentsList.size();i++) {
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
                    agentsList.add(i+1,child);
                    i++;
                } catch (StaleProxyException e) {
                    e.printStackTrace();
                }
            }
        }

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

    private void sendMessageToPerson(ArrayList<Point> neighborsSites, Person person) {

        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.addReceiver(person.getAID());
        ArrayList<String> tmp = new ArrayList<>();

        for(int i = 0; i < neighborsSites.size(); i++) {
            AID aid = this.getPerson(neighborsSites.get(i)).getAID();
            tmp.add(aid.getName());
        }

        message.setContent(tmp.toString());
        this.send(message);
    }

    private ArrayList<Point> getNeighborsPositions(Point location) {
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
        addBehaviour(new WorkingBehaviour());
        System.out.println(getLocalName() + ": starting to work!");
    }

    public void takeDown() {
        System.out.println("This " + getLocalName() + " has ended");
    }

    class WorkingBehaviour extends Behaviour {
        private int n = 0;

        public void action() {
            //System.out.println(++n + " I am doing something!");
            tick();
            /*System.out.println("This is the world state");
            for (Point person: terrain.keySet()){
                String key = person.toString();
                String value = terrain.get(person).toString();
                System.out.println(key + " " + value);
            }*/
            try {
                Thread.sleep(tickDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean done() {
            return false;
        }
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