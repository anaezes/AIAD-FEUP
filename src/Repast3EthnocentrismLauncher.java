import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.*;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DTorus;

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

    @Override
    protected void launchJADE() {
        random = new Random(this.getRngSeed());
        Runtime rt = Runtime.instance();
        Profile p1 = new ProfileImpl();
        mainContainer = rt.createMainContainer(p1);
        try {
            launchAgents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchAgents() throws StaleProxyException {
        //mainContainer.acceptNewAgent("world",new World(agentList,space,random));
        // TODO: 31/10/2018 launch necessary agents
    }

    @Override
    public String[] getInitParam() {
        return new String[] {
                "spaceSize",
                "immigrantsPerDay",
                "initialPtr",
                "deathRate",
                "mutationRate",
                "immigrantChanceCooperateWithSame",
                "immigrantChanceCooperateWithDifferent",
                "tickDelay",
                "plotResolution"};
    }

    @Override
    public String getName() {
        return "Ethnocentrism Model";
    }

    @Override
    public void setup() {
        super.setup();
        setSpaceSize(50);
        setDeathRate(0.1);
        setImmigrantChanceCooperateWithDifferent(0.5);
        setImmigrantChanceCooperateWithSame(0.5);
        setImmigrantsPerDay(10);
        setInitialPtr(0.12);
        setMutationRate(0.05);
        setTickDelay(0);
        plotResolution = 10;
        if (dsurf != null) dsurf.dispose();
        dsurf = new DisplaySurface(this, "World Display");
        registerDisplaySurface("World Display", dsurf);
    }

    @Override
    public void begin() {
        super.begin();
        buildModel();
        buildDisplay();
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
                mainContainer);
        try {
            mainContainer.acceptNewAgent("world",world).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    public void buildModel() {
        agentList = new ArrayList<>();
        space = new Object2DTorus(spaceSize, spaceSize);

        // graph
        if (plot != null) plot.dispose();
        plot = new OpenSequenceGraph("Population", this);
        plot.setAxisTitles("Ticks", "Nr of people");

        // plot number of people that coopSame and coopDiff
        plot.addSequence("CC", new Sequence() {
            public double getSValue() {
                int result = 0;
                for (Person p: agentList) {
                    if (p.getCooperateWithSame() && p.getCooperateWithDifferent()){
                        result++;
                    }
                }
                return result;
            }
        });
        // plot number of people that coopSame and !coopDiff
        plot.addSequence("CD", new Sequence() {
            public double getSValue() {
                int result = 0;
                for (Person p: agentList) {
                    if (p.getCooperateWithSame() && !p.getCooperateWithDifferent()){
                        result++;
                    }
                }
                return result;
            }
        });

        // plot number of people that !coopSame and coopDiff
        plot.addSequence("DC", new Sequence() {
            public double getSValue() {
                int result = 0;
                for (Person p: agentList) {
                    if (!p.getCooperateWithSame() && p.getCooperateWithDifferent()){
                        result++;
                    }
                }
                return result;
            }
        });

        // plot number of people that !coopSame and !coopDiff
        plot.addSequence("DD", new Sequence() {
            public double getSValue() {
                int result = 0;
                for (Person p: agentList) {
                    if (!p.getCooperateWithSame() && !p.getCooperateWithDifferent()){
                        result++;
                    }
                }
                return result;
            }
        });

        plot.display();
    }

    private void buildDisplay() {
        // space and display surface
        Object2DDisplay display = new Object2DDisplay(space);
        display.setObjectList(agentList);
        dsurf.addDisplayableProbeable(display, "Agents Space");
        dsurf.display();
    }

    private void buildSchedule() {

        getSchedule().scheduleActionAtInterval(1, dsurf, "updateDisplay", Schedule.LAST);
        getSchedule().scheduleActionAtInterval(plotResolution, plot, "step", Schedule.LAST);
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
        if(world!=null){
            world.setImmigrantsPerDay(immigrantsPerDay);
        }
    }

    public double getInitialPtr() {
        return initialPtr;
    }

    public void setInitialPtr(double initialPtr) {
        this.initialPtr = initialPtr;
        if(world!=null){
            world.setInitialPtr(initialPtr);
        }
    }

    public double getDeathRate() {
        return deathRate;
    }

    public void setDeathRate(double deathRate) {
        this.deathRate = deathRate;
        if(world!=null){
            world.setDeathRate(deathRate);
        }
    }

    public double getMutationRate() {
        return mutationRate;
    }

    public void setMutationRate(double mutationRate) {
        this.mutationRate = mutationRate;
        if(world!=null){
            world.setMutationRate(mutationRate);
        }
    }

    public double getImmigrantChanceCooperateWithSame() {
        return immigrantChanceCooperateWithSame;
    }

    public void setImmigrantChanceCooperateWithSame(double immigrantChanceCooperateWithSame) {
        this.immigrantChanceCooperateWithSame = immigrantChanceCooperateWithSame;
        if(world!=null){
            world.setImmigrantChanceCooperateWithSame(immigrantChanceCooperateWithSame);
        }

    }

    public double getImmigrantChanceCooperateWithDifferent() {
        return immigrantChanceCooperateWithDifferent;
    }

    public void setImmigrantChanceCooperateWithDifferent(double immigrantChanceCooperateWithDifferent) {
        this.immigrantChanceCooperateWithDifferent = immigrantChanceCooperateWithDifferent;
        if(world!=null){
            world.setImmigrantChanceCooperateWithDifferent(immigrantChanceCooperateWithDifferent);
        }
    }

    public int getTickDelay() {
        return tickDelay;
    }

    public void setTickDelay(int tickDelay) {
        this.tickDelay = tickDelay;
        if(world!=null){
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
