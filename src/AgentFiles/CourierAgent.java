package AgentFiles;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetResponder;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CourierAgent extends Agent implements Serializable {
    private final int velocity = 40; //velocity Km/h
    private int maxWorkHoursPerDay;
    private int maxCapacity;
    private Location storeLocation; //start and end of the trajectory
    private AID storeAID;

    private List<Product> listOfDeliveries;
    private int usedCapacity = 0;

    public void setup() {
        Object[] args = getArguments();
        maxWorkHoursPerDay = (int) args[0];
        storeLocation = (Location) args[1];
        maxCapacity = (int) args[2];
        listOfDeliveries = new ArrayList<>(maxCapacity);
        storeAID = (AID) args[3];

        System.out.println("[" + this.getLocalName() + "] Courier created.");

        addBehaviour(new CourierCheckIn(this.getAID())); //Check in too store
        addBehaviour(new FIPAContractNetResp(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));
    }

    /*
        returns the time added with that delivery
     */
    private float addDelivery(Product newProduct) { //TODO change return type into TIMESTAMP???

        if(usedCapacity + newProduct.getVolume() > maxCapacity) return -1;

        float initialTime = calculateTotalTime();

        float distance = 0;
        //the position of the addition doesn't matter when the size is 0 or 1
        if(listOfDeliveries.size() == 0) {
            listOfDeliveries.add(newProduct);
            distance = 2 * Location.manhattanDistance(storeLocation, newProduct.getDeliveryLocation());

        } /* else if( listOfDeliveries.size() == 1) {
            listOfDeliveries.add(newProduct);
            distance = Location.manhattanDistance(storeLocation, listOfDeliveries.get(0).getDeliveryLocation()) +
                    Location.manhattanDistance(listOfDeliveries.get(0).getDeliveryLocation(), newProduct.getDeliveryLocation()) +
                    Location.manhattanDistance(newProduct.getDeliveryLocation(), storeLocation);

        } */ else {

            int finalPosition = 0;

            for (int i = 0; i < listOfDeliveries.size(); i++) {
                List<Product> productsCopy = new ArrayList<>(listOfDeliveries);
                int tmpDistance = 0;

                //copy by value
                productsCopy.add(i, newProduct);

                tmpDistance += Location.manhattanDistance(storeLocation, productsCopy.get(0).getDeliveryLocation());
                tmpDistance += Location.manhattanDistance(productsCopy.get(productsCopy.size() - 1).getDeliveryLocation(), storeLocation);

                for (int j = 0; j < productsCopy.size() - 1; j++) {
                    tmpDistance += Location.manhattanDistance(productsCopy.get(j).getDeliveryLocation(), productsCopy.get(j + 1).getDeliveryLocation());
                }

                //check if it was instantiated
                if (distance == -1 || distance > tmpDistance) {
                    distance = tmpDistance;
                    finalPosition = i;
                }
            }


            listOfDeliveries.add(finalPosition, newProduct);
            usedCapacity += newProduct.getVolume();
        }

        float totalTime = distance/velocity;

        if(totalTime > maxWorkHoursPerDay) {
            listOfDeliveries.remove(newProduct);
            return -1;
        }
        else return (totalTime - initialTime);
    }

    private float calculateTotalTime() { //Returns time in hours
        int distance = 0;
        if(listOfDeliveries.size() == 0) {
            return 0;
        } else if( listOfDeliveries.size() == 1) {
            distance = 2 * Location.manhattanDistance(storeLocation, listOfDeliveries.get(0).getDeliveryLocation());
        } else {
            distance += Location.manhattanDistance(storeLocation, listOfDeliveries.get(0).getDeliveryLocation());
            distance += Location.manhattanDistance(listOfDeliveries.get(listOfDeliveries.size() - 1).getDeliveryLocation(), storeLocation);

            for (int j = 0; j < listOfDeliveries.size() - 1; j++) {
                distance += Location.manhattanDistance(listOfDeliveries.get(j).getDeliveryLocation(), listOfDeliveries.get(j + 1).getDeliveryLocation());
            }
        }

        return distance/velocity;
    }

    class CourierCheckIn extends Behaviour {
        boolean checkedIn = false;
        private AID courierAID;

        CourierCheckIn(AID courierAID) {
            this.courierAID = courierAID;
        }

        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.SUBSCRIBE);
            try {
                System.out.println("[" + courierAID.getLocalName() + "] Checking in...");
                msg.setContentObject(courierAID);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("[" + courierAID.getLocalName() + "] Couldn't send check-In message with Courier: " + this);
                checkedIn = true; //Change this
                return;
            }
            msg.addReceiver(storeAID);
            send(msg);

            checkedIn = true; //TODO Change this to true only when we receive a response
        }

        @Override
        public boolean done() {
            return checkedIn;
        }
    }

    class FIPAContractNetResp extends ContractNetResponder {
        FIPAContractNetResp(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        protected ACLMessage handleCfp(ACLMessage cfp) {
            Product product;
            ACLMessage reply = cfp.createReply();
            try {
                product = (Product) cfp.getContentObject();
            } catch (UnreadableException e) {
                System.err.println("[" + this.getAgent().getLocalName() + "] Couldn't get ContentObject from " + cfp);

                reply.setPerformative(ACLMessage.REFUSE);
                return reply;
            }

            float timeTillDelivery = addDelivery(product);
            if(timeTillDelivery == -1) {
                System.out.println("[" + this.getAgent().getLocalName() + "] Cannot fulfill request.");
                reply.setPerformative(ACLMessage.REFUSE);
            }
            else {
                System.out.println("[" + this.getAgent().getLocalName() + "] Proposing value " + timeTillDelivery + ".");
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(String.valueOf(timeTillDelivery));
            }
            return reply;
        }

        protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
            return;
        }

        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
            System.out.println("[" + this.getAgent().getLocalName() + "] Confirming delivery...");

            ACLMessage result = accept.createReply();
            result.setPerformative(ACLMessage.INFORM);
            result.setContent("Confirmed delivery");

            return result;
        }

    }
}
