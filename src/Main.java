import uchicago.src.sim.engine.SimInit;

public class Main {

    public static void main(String[] args) {
        SimInit init = new SimInit();
        init.loadModel(new Repast3EthnocentrismLauncher(), null, false);
    }
}
