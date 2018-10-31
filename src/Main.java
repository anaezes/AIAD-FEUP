import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import uchicago.src.sim.engine.SimInit;

import java.util.Random;

public class Main {

    public static void main(String[] args) {
        Random r = new Random();
        Runtime rt = Runtime.instance();

        SimInit init = new SimInit();
        init.loadModel(new EthnocentrismModel(), null, false);
    }
}
