import uchicago.src.reflector.ListPropertyDescriptor;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DTorus;

import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

public class EthnocentrismModel extends SimModelImpl {

    private Schedule schedule;
    private Object2DTorus space;
    private ArrayList<Person> agentList;
    private DisplaySurface dsurf;


    public int getSpaceSize() {
        return spaceSize;
    }

    public void setSpaceSize(int spaceSize) {
        this.spaceSize = spaceSize;
    }

    public Double getInitialPtr() {
        return initialPtr;
    }

    public void setInitialPtr(Double initialPtr) {
        this.initialPtr = initialPtr;
    }

    private int spaceSize;
    private Double initialPtr;

    public EthnocentrismModel(){
        spaceSize = 50;
        initialPtr = 0.12;
    }

    @Override
    public String[] getInitParam() {
        return new String[] { "spaceSize",
                "initialPTR"};
    }

    @Override
    public void begin() {
        buildModel();
        buildDisplay();
        buildSchedule();

    }

    public void buildModel() {
        agentList = new ArrayList<>();
        space = new Object2DTorus(spaceSize, spaceSize);
    }

    private void buildDisplay() {
        // space and display surface
        Object2DDisplay display = new Object2DDisplay(space);
        display.setObjectList(agentList);
        dsurf.addDisplayableProbeable(display, "Agents Space");
        dsurf.display();
    }

    private void buildSchedule() {
        schedule.scheduleActionBeginning(0, new ImmigrationAction());
        schedule.scheduleActionAtInterval(1, dsurf, "updateDisplay", Schedule.LAST);
    }

    @Override
    public void setup() {
        schedule = new Schedule();
        if (dsurf != null) dsurf.dispose();
        dsurf = new DisplaySurface(this, "World Display");
        registerDisplaySurface("World Display", dsurf);
    }

    @Override
    public Schedule getSchedule() {
        return schedule;
    }

    @Override
    public String getName() {
        return "Ethnocentrism Model";
    }

    class ImmigrationAction extends BasicAction {

        @Override
        public void execute() {
            Random rand = new Random();
            int x = rand.nextInt(spaceSize);
            int y = rand.nextInt(spaceSize);
            Person p = new Person(0.12,Colour.BLUE,true,true, new Point(x,y));
            agentList.add(p);
        }
    }

}
