import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DTorus;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class Repast3EthnocentrismLauncher extends Repast3Launcher {
    private ContainerController mainContainer;
    private int spaceSize;
    private int immigrantsPerDay;
    private double initialPtr;
    private double deathRate;
    private double mutationRate;
    private double immigrantChanceCooperateWithSame;
    private double immigrantChanceCooperateWithDifferent;
    private int tickDelay;
    private Object2DTorus space;
    private ArrayList<Person> agentList;
    private DisplaySurface dsurf;
    private Random random;
    private World world;
    private OpenSequenceGraph plot;
    private int plotResolution;
    private boolean smartChoice;
    private PrintWriter pw;

    @Override
    protected void launchJADE() {
        Runtime rt = Runtime.instance();
        Profile p1 = new ProfileImpl();
        mainContainer = rt.createMainContainer(p1);
    }

    @Override
    public String[] getInitParam() {
        return new String[]{
                "spaceSize",
                "immigrantsPerDay",
                "initialPtr",
                "deathRate",
                "mutationRate",
                "immigrantChanceCooperateWithSame",
                "immigrantChanceCooperateWithDifferent",
                "tickDelay",
                "plotResolution",
                "smartChoice"};
    }

    @Override
    public String getName() {
        return "Ethnocentrism Model";
    }

    public boolean isSmartChoice() {
        return smartChoice;
    }

    public void setSmartChoice(boolean smartChoice) {
        this.smartChoice = smartChoice;
        if (world != null) {
            world.setSmartChoice(smartChoice);
        }
    }

    private void setPrintWriter() {
        try {
            pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream("data.csv")), true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        pw.println("ImmigrantChanceCooperateWithDifferent, ImmigrantChanceCooperateWithSame, InitialPtr, MutationRate, SmartChoice, CC, CD, DC, DD, OneNeighbour, TwoNeighbours, ThreeNeighbours, FourNeighbours, Delta");
    }

    public void printPopulations() {
        pw.printf(", %d, %d, %d, %d", getCCCount(), getCDCount(), getDCCount(), getDDCount());
        int[] sameColourNeighboursCount = getSameColourNeighboursCount();
        pw.printf(", %d, %d, %d, %d", sameColourNeighboursCount[0], sameColourNeighboursCount[1], sameColourNeighboursCount[2], sameColourNeighboursCount[3]);
    }

    public void printDelta() {
        pw.println(", " + (getCCCount() - getCDCount()));
        this.stop();
    }

    @Override
    public void setup() {
        if (pw == null) {
            setPrintWriter();
        }
        this.random = new Random(this.getRngSeed());
        super.setup();
        setSpaceSize(50);
        setDeathRate(0.1);
        setImmigrantChanceCooperateWithDifferent(this.random.nextDouble()); // DEFAULT -> 0.5
        pw.print(getImmigrantChanceCooperateWithDifferent());
        setImmigrantChanceCooperateWithSame(this.random.nextDouble()); // DEFAULT -> 0.5
        pw.print(", " + getImmigrantChanceCooperateWithSame());
        setImmigrantsPerDay(1);
        setInitialPtr(this.random.nextDouble() * (0.2 - 0.04) + 0.04); // number between 0.2 and 0.04 DEFAULT -> 0.12
        pw.print(", " + getInitialPtr());
        setMutationRate(this.random.nextDouble() * 0.01); // number between 0.01 and 0 DEFAULT -> 0.005
        pw.print(", " + getMutationRate());
        setTickDelay(0);
        setSmartChoice(this.random.nextDouble() >= 0.5);
        pw.print(", " + isSmartChoice());
        plotResolution = 10;
    }

    @Override
    public void begin() {
        super.begin();
        buildModel();
//        buildDisplay();
        buildSchedule();
        world = new World(agentList,
                space,
                random,
                spaceSize,
                spaceSize,
                initialPtr,
                deathRate,
                mutationRate,
                immigrantsPerDay,
                immigrantChanceCooperateWithSame,
                immigrantChanceCooperateWithDifferent,
                tickDelay,
                smartChoice,
                plot);
        try {
            mainContainer.acceptNewAgent("world", world).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    public int[] getSameColourNeighboursCount() {
        int[] result = new int[4];
        for (Person p : agentList) {
            int neighboursOfSameColour = 0;
            for (Person n : world.getNeighbours(p.getLocation())) {
                if (p.getColour().equals(n.getColour())) {
                    result[neighboursOfSameColour++]++;
                }
            }
        }
        return result;
    }

    public int getCCCount() {
        int result = 0;
        for (Person p : agentList) {
            if (p.getCooperateWithSame() && p.getCooperateWithDifferent()) {
                result++;
            }
        }
        return result;
    }

    public int getCDCount() {
        int result = 0;
        for (Person p : agentList) {
            if (p.getCooperateWithSame() && !p.getCooperateWithDifferent()) {
                result++;
            }
        }
        return result;
    }

    public int getDCCount() {
        int result = 0;
        for (Person p : agentList) {
            if (!p.getCooperateWithSame() && p.getCooperateWithDifferent()) {
                result++;
            }
        }
        return result;
    }

    public int getDDCount() {
        int result = 0;
        for (Person p : agentList) {
            if (!p.getCooperateWithSame() && !p.getCooperateWithDifferent()) {
                result++;
            }
        }
        return result;
    }

    public void buildModel() {
        agentList = new ArrayList<>();
        space = new Object2DTorus(spaceSize, spaceSize);

        // graph
        if (plot != null) plot.dispose();
        plot = new OpenSequenceGraph("Population", this);
        plot.setAxisTitles("World cycles", "# of people");

        // plot number of people that coopSame and coopDiff
        plot.addSequence("Green - CC", this::getCCCount, Color.GREEN, 10);
        // plot number of people that coopSame and !coopDiff
        plot.addSequence("Red - CD", this::getCDCount, Color.RED, 10);
        // plot number of people that !coopSame and coopDiff
        plot.addSequence("Orange - DC", this::getDCCount, Color.ORANGE, 10);
        // plot number of people that !coopSame and !coopDiff
        plot.addSequence("Gray - DD", this::getDDCount, Color.DARK_GRAY, 10);

        plot.display();
    }


    private void buildDisplay() {
        if (dsurf != null) dsurf.dispose();
        dsurf = new DisplaySurface(this, "World Display");
        registerDisplaySurface("World Display", dsurf);
        // space and display surface
        Object2DDisplay display = new Object2DDisplay(space);
        display.setObjectList(agentList);
        dsurf.addDisplayableProbeable(display, "Agents Space");
        dsurf.display();
    }

    private void buildSchedule() {
//        getSchedule().scheduleActionAtInterval(1, dsurf, "updateDisplay", Schedule.LAST);
        getSchedule().scheduleActionAt(1000, this, "printDelta");
//        this.generateNewSeed();
//        getSchedule().scheduleActionAtEnd(this, "actionPause");
        getSchedule().scheduleActionAt(2, this, "printPopulations", Schedule.LAST);
//        getSchedule().scheduleActionAtInterval(plotResolution, plot, "step", Schedule.LAST);
    }

    public void actionPause() {
//        System.out.println("working fine");
//        this.plot.setSnapshotFileName("test1");
//        this.plot.writeToFile();
//        this.clearSpace();
//        this.agentList.clear();
//        this.world.reset();
//        this.dsurf.removeAll();
//        System.out.println(this.plot.toString());
    }

    public void clearSpace() {
        for (int i = 0; i < agentList.size(); i++) {
            this.space.putObjectAt(agentList.get(i).getX(), agentList.get(i).getY(), null);
        }
        this.world.space = this.space;
    }

    public int getSpaceSize() {
        return spaceSize;
    }

    public void setSpaceSize(int spaceSize) {
        this.spaceSize = spaceSize;
    }

    public Integer getImmigrantsPerDay() {
        return immigrantsPerDay;
    }

    public void setImmigrantsPerDay(Integer immigrantsPerDay) {
        this.immigrantsPerDay = immigrantsPerDay;
        if (world != null) {
            world.setImmigrantsPerDay(immigrantsPerDay);
        }
    }

    public double getInitialPtr() {
        return initialPtr;
    }

    public void setInitialPtr(double initialPtr) {
        this.initialPtr = initialPtr;
        if (world != null) {
            world.setInitialPtr(initialPtr);
        }
    }

    public double getDeathRate() {
        return deathRate;
    }

    public void setDeathRate(double deathRate) {
        this.deathRate = deathRate;
        if (world != null) {
            world.setDeathRate(deathRate);
        }
    }

    public double getMutationRate() {
        return mutationRate;
    }

    public void setMutationRate(double mutationRate) {
        this.mutationRate = mutationRate;
        if (world != null) {
            world.setMutationRate(mutationRate);
        }
    }

    public double getImmigrantChanceCooperateWithSame() {
        return immigrantChanceCooperateWithSame;
    }

    public void setImmigrantChanceCooperateWithSame(double immigrantChanceCooperateWithSame) {
        this.immigrantChanceCooperateWithSame = immigrantChanceCooperateWithSame;
        if (world != null) {
            world.setImmigrantChanceCooperateWithSame(immigrantChanceCooperateWithSame);
        }

    }

    public double getImmigrantChanceCooperateWithDifferent() {
        return immigrantChanceCooperateWithDifferent;
    }

    public void setImmigrantChanceCooperateWithDifferent(double immigrantChanceCooperateWithDifferent) {
        this.immigrantChanceCooperateWithDifferent = immigrantChanceCooperateWithDifferent;
        if (world != null) {
            world.setImmigrantChanceCooperateWithDifferent(immigrantChanceCooperateWithDifferent);
        }
    }

    public int getTickDelay() {
        return tickDelay;
    }

    public void setTickDelay(int tickDelay) {
        this.tickDelay = tickDelay;
        if (world != null) {
            world.setTickDelay(tickDelay);
        }
    }

    public int getPlotResolution() {
        return plotResolution;
    }

    public void setPlotResolution(int plotResolution) {
        this.plotResolution = plotResolution;
    }
}
