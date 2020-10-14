import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

import java.util.List;

public class Courier extends Agent {
    private int productCapacity, maxWorkHoursPerDay;
    List<Product> listOfDeliveries;

    public void setup() {
        System.out.println("Setting up Courier Agent");
        addBehaviour(new Behaviour(this));
    }

    class Behaviour extends SimpleBehaviour {
        private boolean finished = false;

        Behaviour(Agent agent) {
            super(agent);
        }

        @Override
        public void action() {
            System.out.println("This Courier's name is: " + myAgent.getLocalName());
            finished = true;
        }

        @Override
        public boolean done() {
            return finished;
        }
    }
}
