import Agents.CourierAgent;
import Agents.StoreAgent;
import AuxiliaryClasses.AlgorithmUsed;
import AuxiliaryClasses.Location;
import AuxiliaryClasses.Product;

import jade.core.AID;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.core.Profile;
import sajas.core.Runtime;
import sajas.wrapper.AgentController;
//import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Network2DDisplay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

class Repast3StoreLauncher  extends Repast3Launcher {
    private static Random generator = new Random(123456);
    private static final Location storeLocation = new Location(0,0);
    private static int numCouriers;
    private static int numPackages;
    private static int algorithm;
    private static StoreAgent storeAgent;

    @Override
    public String[] getInitParam() {
        return new String[0];
    }

    @Override
    public String getName() {
        return "Store -- SAJaS Repast3";
    }

    /**
     * Launching Repast3
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Wrong usage: java JadeInit <num_couriers> <num_packages> <algorithm(0/1/2)>");
            return;
        }

        try {
            numCouriers = Integer.parseInt(args[0]);
            numPackages = Integer.parseInt(args[1]);
            algorithm = Integer.parseInt(args[2]);
            if (numCouriers <= 0) {
                System.err.println("Must have 1 or more couriers");
                return;
            }

            if (numPackages <= 0) {
                System.err.println("Must have 1 or more packages");
                return;
            }

            if (algorithm < 0 || algorithm > 2) {
                System.err.println("Invalid Algorithm, choose from 0, 1 or 2");
                return;
            }
        } catch (Exception e) {
            System.err.println("Arguments provided are not valid, aborting...");
            return;
        }

        SimInit init = new SimInit();
        init.setNumRuns(1);   // works only in batch mode
        init.loadModel(new Repast3StoreLauncher(), null, false);
    }

    @Override
    protected void launchJADE() {

        //Start JADE
        Runtime runt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");

        //Create Main Container
        ContainerController mainContainer = runt.createMainContainer(profile);

        //Add agents
        AgentController storeController;
        String storeName = "Store";
        List<Product> storeProducts = createProducts(numPackages);
        storeAgent = new StoreAgent(storeProducts, numCouriers);

        /*
        Object storeArgs[] = new Object[2];
        storeArgs[0] = storeProducts;
        storeArgs[1] = numCouriers;
        */

        try {
            // storeController = mainContainer.createNewAgent(storeName, "Agents.StoreAgent", storeArgs);
            storeController = mainContainer.acceptNewAgent(storeName,storeAgent);
            storeController.start();
        } catch (StaleProxyException e) {
            System.err.println("\nThere was an error creating the agent!");
            e.printStackTrace();
            return;
        }
        System.out.println("[Main] Store Agent created...");

        List<AgentController> courierControllers = createCouriers(numCouriers, mainContainer, algorithm);
        System.out.println("[Main] Courier Agents created...");

        System.out.println("[Main] Container Running....");
        return;
    }

    private static List<Product> createProducts(int numProducts) {
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= numProducts; i++) {
            Product newProduct = new Product(i, new Location(generator.nextInt(21) - 10, generator.nextInt(21) - 10), generator.nextInt(3) + 1);
            products.add(newProduct);
        }
        return products;
    }

    private static List<AgentController> createCouriers(int numCouriers, ContainerController mainContainer, int algorithm) {
        List<AgentController> courierControllers = new ArrayList<>();
        for(int i = 1; i <= numCouriers; i++) {
            int hours = new Random().nextInt(2) + 8;
            Object[] args = new Object[5];
            args[0] = hours;
            args[1] = storeLocation;
            args[3] = AlgorithmUsed.values()[algorithm];

            List<Integer> possibleCapacities = Arrays.asList(9, 12, 15);
            args[2] = 15;//possibleCapacities.get(generator.nextInt(3));;

            try {
                AgentController courierController = mainContainer.createNewAgent("Courier"+i,"Agents.CourierAgent", args);
                courierController.start();
                courierControllers.add(courierController);
                courierController.start();
            } catch (StaleProxyException e) {
                System.err.println("Couldn't Create Courier Agent: " + i);
            }
        }

        return courierControllers;
    }

    // create graphs
    @Override
    public void begin() {
        super.begin();
        buildAndScheduleDisplay();
    }

    private void buildAndScheduleDisplay() {
        /*
        // THISI IS FOR THE VISUAL EXECUTIOON GRAPH (ATÉ ERA GIRO TERMOS)

        int WIDTH = 200, HEIGHT = 200;
        DisplaySurface dsurf = null;
        if (dsurf != null) dsurf.dispose();
        dsurf = new DisplaySurface(this, "Total Distance Display");
        registerDisplaySurface("Total Distance Display", dsurf);
        Network2DDisplay display = new Network2DDisplay(nodes,WIDTH,HEIGHT);
        dsurf.addDisplayableProbeable(display, "Network Display");
        dsurf.addZoomable(display);
        addSimEventListener(dsurf);
        dsurf.display();
        */

        // total Distance Graph
        OpenSequenceGraph totalDistPlot = null;
        if (totalDistPlot != null) totalDistPlot.dispose();
        totalDistPlot = new OpenSequenceGraph("Total Distance", this);
        totalDistPlot.setAxisTitles("time", "Total km");

        totalDistPlot.addSequence("Courier Distance", () -> storeAgent.getTotalSystemDistance());
        totalDistPlot.display();

        // Time to deliver package Graph
        OpenSequenceGraph timePerPackagePlot = null;
        if (timePerPackagePlot != null) timePerPackagePlot.dispose();
        timePerPackagePlot = new OpenSequenceGraph("Time Per package", this);
        timePerPackagePlot.setAxisTitles("time", "Avg");

        timePerPackagePlot.addSequence("Package Time", () -> storeAgent.getPackageAvgTime());
        timePerPackagePlot.display();

        // getSchedule().scheduleActionAtInterval(1, dsurf, "updateDisplay", Schedule.LAST);
        getSchedule().scheduleActionAtInterval(100, totalDistPlot, "step", Schedule.LAST);
        getSchedule().scheduleActionAtInterval(100, timePerPackagePlot, "step", Schedule.LAST);
    }

}
