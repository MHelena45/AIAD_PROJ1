package AgentFiles;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

public class DummyAgent extends Agent {

    public void setup() {
        System.out.println("Hi, I'm Dummy!");
        addBehaviour(new Behaviour(this));
    }

    class Behaviour extends SimpleBehaviour {
        private boolean finished = false;

        public Behaviour(Agent agent) {
            super(agent);
        }

        @Override
        public void action() {
            System.out.println("My name is: " + myAgent.getLocalName());
            finished = true;
        }

        @Override
        public boolean done() {
            return finished;
        }
    }
}