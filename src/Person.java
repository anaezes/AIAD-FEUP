import jade.lang.acl.ACLMessage;
import sajas.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import java.awt.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class Person extends Agent implements Drawable {

    private static final double INCREASE_VALUE_PTR = 0.03;
    private static final double DECREASE_VALUE_PTR = 0.01;

    private Double ptr;
    private Colour colour;
    private Boolean cooperateWithSame;
    private Boolean cooperateWithDifferent;
    private Point location;
    private UUID id;

    private Integer neighbourCount;
    private jade.core.AID world;

    private HashSet<String> isBad;

    private boolean smartChoice;

    public Person(Double ptr, Colour colour, Boolean cooperateWithSame, Boolean cooperateWithDifferent, Point location, boolean smartChoice) {
        this.ptr = ptr;
        this.colour = colour;
        this.cooperateWithSame = cooperateWithSame;
        this.cooperateWithDifferent = cooperateWithDifferent;
        this.location = location;
        this.id = UUID.randomUUID();
        this.isBad = new HashSet<>();
        this.smartChoice = smartChoice;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

        return new Person(null, colour, cooperateWithSame, cooperateWithDifferent, null, smartChoice);
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
        if (cooperateWithDifferent) {
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
        return (int) location.getX();
    }

    @Override
    public int getY() {
        return (int) location.getY();
    }

    public Boolean getCooperateWithSame() {
        return cooperateWithSame;
    }

    public Boolean getCooperateWithDifferent() {
        return cooperateWithDifferent;
    }

    public void setup() {
        addBehaviour(new behaviour());
    }

    private void processAcceptProposal() {
        this.setPtr(this.ptr + INCREASE_VALUE_PTR);
    }

    public boolean isSmartChoice() {
        return smartChoice;
    }

    public void setSmartChoice(boolean smartChoice) {
        this.smartChoice = smartChoice;
    }

    private void sendResponse(ACLMessage message, boolean collaborate) {
        ACLMessage response = message.createReply();

        if (collaborate) {
            this.setPtr(this.ptr - DECREASE_VALUE_PTR);
            response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
        } else {
            response.setPerformative(ACLMessage.REJECT_PROPOSAL);
        }

        /*System.out.println(getName() + ": " + message.getSender().getName() + " proposed.");
        System.out.println("my colour: " + colour + " their colour: " + message.getContent());
        System.out.println("help same: " + cooperateWithSame + " help others: " + cooperateWithDifferent);
        System.out.println("decision: " + collaborate);*/

        response.addReplyTo(this.getAID());
        this.send(response);
    }

    private boolean studyProposal(String content, String sender) {
        if (smartChoice && isBad.contains(sender)) {
            return false;
        }

        //System.out.println("colour cmp: "+colour.toString().equals(content));
        if (cooperateWithSame && colour.toString().equals(content)) {
            return true;
        }

        if (cooperateWithDifferent && !colour.toString().equals(content)) {
            return true;
        }

        return false;
    }

    private void proposeNeighbours(String[] neighbours) {
        neighbourCount = neighbours.length;
        for (int i = 0; i < neighbours.length; i++) {
            final AID r = new AID(neighbours[i], true);
            //System.out.println(getName() + ": neighbour " + r.getName());
            ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
            message.addReceiver(r);
            message.setContent(colour.toString());
            message.addReplyTo(this.getAID());
            this.send(message);
        }
    }

    private class behaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage message = receive();

            if (message == null) {
                return;
            }

            int type = message.getPerformative();

            switch (type) {
                case ACLMessage.INFORM:
                    world = message.getSender();
                    proposeNeighbours(message.getContent().split(","));
                    break;
                case ACLMessage.PROPOSE:
                    boolean cooperation = studyProposal(message.getContent(), message.getSender().getName());
                    sendResponse(message, cooperation);
                    break;
                case ACLMessage.ACCEPT_PROPOSAL:
                    //System.out.println("Receive ACCEPT_PROPOSAL!\n");
                    processAcceptProposal();
                    neighbourCount--;
                    break;
                case ACLMessage.REJECT_PROPOSAL:
                    //System.out.println("Receive REJECT_PROPOSAL!\n");
                    isBad.add(message.getSender().getName());
                    neighbourCount--;
                    break;
                default:
                    //do nothing
            }

            //System.out.println(getName() + ": " + neighbourCount);
            if (neighbourCount.equals(0)) {
                //System.out.println("sending response to world");
                ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                reply.addReceiver(world);
                reply.setContent("person ID"+getName());
                send(reply);
            }
        }

    }
}
