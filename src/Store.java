import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

public class Store extends Agent {
    public void setup() {
        System.out.println("Setting up Store Agent");
        addBehaviour(new Behaviour(this));
    }

    class Behaviour extends SimpleBehaviour {
        private boolean finished = false;

        Behaviour(Agent agent) {
            super(agent);
        }

        @Override
        public void action() {
            System.out.println("This Store's name is: " + myAgent.getLocalName());
            finished = true;
        }

        @Override
        public boolean done() {
            return finished;
        }
    }
}
