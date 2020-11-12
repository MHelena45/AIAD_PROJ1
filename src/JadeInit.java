import AgentFiles.Location;
import jade.core.AID;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.core.Profile;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JadeInit {
    private static final Location storeLocation = new Location(0,0);
    public static void main(String[] args) {
        //Start JADE
        Runtime runt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");

        //Create Main Container
        AgentContainer mainContainer = runt.createMainContainer(profile);

        //Add agents
        AgentController storeController;
        String storeName = "Dina's store";
        try {
            storeController = mainContainer.createNewAgent(storeName, "AgentFiles.StoreAgent", null);
            storeController.start();
        } catch (StaleProxyException e) {
            System.err.println("\nThere was an error creating the agent!");
            e.printStackTrace();
            return;
        }
        System.out.println("Store Agent created...");

        AID storeAID = new AID(storeName, AID.ISLOCALNAME);
        List<AgentController> courierControllers = createCouriers(5, mainContainer, storeAID);
        System.out.println("Courier Agents created...");

        try {
            mainContainer.start();
        } catch (ControllerException e) {
            System.err.println("\nThere was an error with the main Container!");
            e.printStackTrace();
        }

        System.out.println("Container Running....");
    }

    private static List<AgentController> createCouriers(int numCouriers, AgentContainer mainContainer, AID storeAID) {
        List<AgentController> courierControllers = new ArrayList<>();
        for(int i = 1; i <= numCouriers; i++) {
            int hours = new Random().nextInt(2) + 8;
            Object[] args = new Object[4];
            args[0] = hours; args[1] = storeLocation; args[2] = storeAID;

            try {
                AgentController courierController = mainContainer.createNewAgent("Courier"+i,"AgentFiles.CourierAgent", args);
                courierControllers.add(courierController);
                courierController.start();
            } catch (StaleProxyException e) {
                System.err.println("Couldn't Create Courier Agent: " + i);
            }
        }

        return courierControllers;
    }
}
