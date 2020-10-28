package AgentFiles;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

import java.util.List;
import java.util.Vector;

public class StoreAgent extends Agent {
    private Location storeLocation;
    List<Product> listOfOrders;

    public void setup() {
        System.out.println("Setting up StoreAgent");
        storeLocation = new Location(0,0);

        addBehaviour(new Behaviour(this));
        addBehaviour(new FIPAContractNetInit(this, new ACLMessage(ACLMessage.CFP))); //This should be called when sending out a delivery request
    }

    class FIPAContractNetInit extends ContractNetInitiator {

        public FIPAContractNetInit(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        protected Vector prepareCfps(ACLMessage cfp) {
            Vector v = new Vector();

            cfp.addReceiver(new AID("a1", false));
            cfp.addReceiver(new AID("a2", false));
            cfp.addReceiver(new AID("a3", false));
            cfp.setContent("this is a call...");

            v.add(cfp);

            return v;
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {

            System.out.println("got " + responses.size() + " responses!");

            for(int i=0; i<responses.size(); i++) {
                ACLMessage msg = ((ACLMessage) responses.get(i)).createReply();
                msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL); // OR NOT!
                acceptances.add(msg);
            }
        }

        protected void handleAllResultNotifications(Vector resultNotifications) {
            System.out.println("got " + resultNotifications.size() + " result notifs!");
        }

    }

    class Behaviour extends SimpleBehaviour {
        private boolean finished = false;

        Behaviour(Agent agent) {
            super(agent);
        }

        @Override
        public void action() {
            System.out.println("This StoreAgent's name is: " + myAgent.getLocalName());
            finished = true;
        }

        @Override
        public boolean done() {
            return finished;
        }
    }
}
