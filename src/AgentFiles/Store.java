package AgentFiles;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import java.util.List;

public class Store extends Agent {
    private Location storeLocation;
    List<Product> listOfOrders;

    public void setup() {
        System.out.println("Setting up AgentFiles.Store Agent");
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
            System.out.println("This AgentFiles.Store's name is: " + myAgent.getLocalName());
            finished = true;
        }

        @Override
        public boolean done() {
            return finished;
        }
    }
}
