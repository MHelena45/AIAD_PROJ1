package AgentFiles;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

import java.util.List;

public class Courier extends Agent {
    private final int velocity = 40; //velocity Km/h
    private int maxWorkHoursPerDay;
    private List<Product> listOfDeliveries;
    private Location storeLocation; //start and end of the traject

    public void setup() {
        System.out.println("Setting up AgentFiles.Courier Agent");
        addBehaviour(new Behaviour(this));

        storeLocation = new Location(0,0);
    }

    class Behaviour extends SimpleBehaviour {
        private boolean finished = false;

        Behaviour(Agent agent) {
            super(agent);
        }

        @Override
        public void action() {
            System.out.println("This AgentFiles.Courier's name is: " + myAgent.getLocalName());
            finished = true;
        }

        @Override
        public boolean done() {
            return finished;
        }
    }
}
