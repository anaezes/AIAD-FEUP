import jade.core.Runtime;
import uchicago.src.sim.engine.SimInit;

import java.util.Random;

public class Main {

    public static void main(String[] args) {
        Random r = new Random();
        Runtime rt = Runtime.instance();

        SimInit init = new SimInit();
        init.loadModel(new Repast3EthnocentrismLauncher(), null, false);
    }
}
