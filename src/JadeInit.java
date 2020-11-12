import AgentFiles.Location;
import AgentFiles.Product;
import jade.core.AID;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.core.Profile;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class JadeInit {
    private static Random generator = new Random(1234);
    private static final Location storeLocation = new Location(0,0);
    public static void main(String[] args) {
        if(args.length != 3) {
            System.err.println("Wrong usage: java JadeInit <num_couriers> <num_packages> <algorithm(0/1/2)>");
            return;
        }

        int numCouriers, numPackages;
        try {
            numCouriers = Integer.parseInt(args[0]);
            numPackages = Integer.parseInt(args[1]);
            if(numCouriers <= 0) {
                System.err.println("Must have 1 or more couriers");
                return;
            }

            if(numPackages <= 0) {
                System.err.println("Must have 1 or more packages");
                return;
            }
        } catch (Exception e) {
            System.err.println("Arguments provided are not valid, aborting...");
            return;
        }

        //Start JADE
        Runtime runt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");

        //Create Main Container
        AgentContainer mainContainer = runt.createMainContainer(profile);

        //Add agents
        AgentController storeController;
        String storeName = "Dina's store";
        List<Product> storeProducts = createProducts(numPackages);
        Object storeArgs[] = new Object[1];
        storeArgs[0] = storeProducts;
        try {
            storeController = mainContainer.createNewAgent(storeName, "AgentFiles.StoreAgent", storeArgs);
            storeController.start();
        } catch (StaleProxyException e) {
            System.err.println("\nThere was an error creating the agent!");
            e.printStackTrace();
            return;
        }
        System.out.println("[Main] Store Agent created...");

        AID storeAID = new AID(storeName, AID.ISLOCALNAME);
        List<AgentController> courierControllers = createCouriers(numCouriers, mainContainer, storeAID);
        System.out.println("[Main] Courier Agents created...");

        try {
            mainContainer.start();
        } catch (ControllerException e) {
            System.err.println("\nThere was an error with the main Container!");
            e.printStackTrace();
        }

        System.out.println("[Main] Container Running....");
        return;
    }

    private static List<Product> createProducts(int numProducts) {
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= numProducts; i++) {
            Product newProduct = new Product(i, new Location(generator.nextInt(21) - 10, generator.nextInt(21) - 10), 72000, generator.nextInt(3) + 1);
            products.add(newProduct);
        }
        return products;
    }

    private static List<AgentController> createCouriers(int numCouriers, AgentContainer mainContainer, AID storeAID) {
        List<AgentController> courierControllers = new ArrayList<>();
        for(int i = 1; i <= numCouriers; i++) {
            int hours = new Random().nextInt(2) + 8;
            Object[] args = new Object[4];
            args[0] = hours;
            args[1] = storeLocation;
            args[2] = storeAID;

            generator.nextInt(3);
            List<Integer> possibleCapacities = Arrays.asList(9, 12, 15);
            args[3] = possibleCapacities.get(generator.nextInt(3));;

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
