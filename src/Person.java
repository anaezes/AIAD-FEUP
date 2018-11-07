import jade.core.MessageQueue;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import sajas.core.Agent;
import sajas.core.*;
import sajas.core.behaviours.Behaviour;
import sajas.core.behaviours.CyclicBehaviour;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import java.awt.*;
import java.util.*;

public class Person extends Agent implements Drawable {
    private Double ptr;
    private Colour colour;
    private Boolean cooperateWithSame;
    private Boolean cooperateWithDifferent;
    private Point location;
    //private MessageQueue mailBox = null;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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
        if(cooperateWithDifferent) {
            if (cooperateWithSame) {
                simGraphics.drawHollowFastOval(this.colour.getAWTColor());
            } else {
                simGraphics.drawHollowFastRect(this.colour.getAWTColor());
            }
        } else {
            if (cooperateWithSame) {
                simGraphics.drawFastOval(this.colour.getAWTColor());
            } else {
                simGraphics.drawFastRect(this.colour.getAWTColor());
            }
        }
    }

    @Override
    public int getX() {
        return (int)location.getX();
    }

    @Override
    public int getY() {
        return (int)location.getY();
    }

    public Boolean getCooperateWithSame() {
        return cooperateWithSame;
    }

    public Boolean getCooperateWithDifferent() {
        return cooperateWithDifferent;
    }

    public void setup() {
        this.setColour();
        addBehaviour(new behaviour());

    }

    private class behaviour extends CyclicBehaviour {
        @Override
        public void action() {

            ACLMessage message = receive();
            if(message == null)
                return;

            int type = message.getPerformative();

            //type
            switch (type){
                case ACLMessage.INFORM:
                    String c = message.getContent();
                    String msg = c.substring(1, c.length()-1);
                    String[] neighbors = msg.split(", ");
                    proposeNeighbors(neighbors);

                    System.out.println("Send proposal!\n");
                    break;
                case ACLMessage.PROPOSE:
                    System.out.println("Receive proposal!\n");
                    break;
                case ACLMessage.ACCEPT_PROPOSAL:
                    break;
                case ACLMessage.REJECT_PROPOSAL:
                    break;
                default:
                    //do nothing
            }
        }

    }

    private void proposeNeighbors(String[] neighbors) {

       for(int i = 0; i < neighbors.length; i++) {
           System.out.println("\nNeighbor: " + neighbors[i]);
           ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
           message.addReceiver(AidHolder.getInstance().getAID(neighbors[i]));
           message.setContent(colour.toString());
           this.send(message);
        }
    }

    private void setColour() {
        this.colour = Colour.RED;
    }

}
