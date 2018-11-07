import jade.core.AID;

import java.util.HashMap;

public class AidHolder {
    private static AidHolder instance;
    private HashMap<String, AID> aids;

    private AidHolder() {
        aids = new HashMap<>();
    }

    public static AidHolder getInstance() {
        if(instance == null)
            instance = new AidHolder();
        return instance;
    }

    public void addAID(AID aid){
        synchronized (aids) {
            aids.put(aid.toString(), aid);
        }
    }

    public void removeAID(String aid){
        synchronized (aids) {
            aids.remove(aid);
        }
    }

    public AID getAID(String aid){
        synchronized (aids) {
            return aids.get(aid);
        }
    }
}
