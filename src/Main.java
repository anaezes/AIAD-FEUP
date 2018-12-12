import uchicago.src.sim.engine.SimInit;

import java.util.Locale;

public class Main {

    public static void main(String[] args) {
        Locale.setDefault(new Locale("en", "US"));
        SimInit init = new SimInit();
        init.loadModel(new Repast3EthnocentrismLauncher(), null, false);
    }
}
