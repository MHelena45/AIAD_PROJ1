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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class CourierAgent extends Agent implements Serializable {
    private final int velocity = 40; //velocity Km/h
    private int maxWorkHoursPerDay, algorithm;
    private int maxCapacity;
    private Location storeLocation; //start and end of the trajectory
    private AID storeAID;

    private List<Product> listOfDeliveries;
    private int usedCapacity = 0;

    public void setup() {
        Object[] args = getArguments();
        maxWorkHoursPerDay = (int) args[0];
        storeLocation = (Location) args[1];
        storeAID = (AID) args[2];
        maxCapacity = (int) args[3];
        algorithm = (int) args[4];

        System.out.println("[" + this.getLocalName() + "] Courier created, with capacity " + maxCapacity + ".");

        listOfDeliveries = new ArrayList<>();

        addBehaviour(new CourierCheckIn(this.getAID())); //Check in to the store
        addBehaviour(new FIPAContractNetResp(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));
    }

    /**
     * Adds delivery in the correct place
     * @param product product being
     * @param listOfDeliveries list where product should be added (usually class attribute)
     */
    private void addDelivery(Product product, List<Product> listOfDeliveries) {
        int finalPosition = 0;
        float distance = -1;
        for (int i = 0; i < listOfDeliveries.size(); i++) {
            List<Product> productsCopy = new ArrayList<>(listOfDeliveries);
            productsCopy.add(i, product);
            float tmpDistance = calculateTotalTime(productsCopy);

            //check if it was instantiated or current value is smaller
            if (distance == -1 || distance > tmpDistance) {
                distance = tmpDistance;
                finalPosition = i;
            }
        }

        listOfDeliveries.add(finalPosition, product);

        if(listOfDeliveries.equals(this.listOfDeliveries)) this.usedCapacity += product.getVolume(); //only if we're adding to the class list
    }

    /**
     * checks if the courier can accept the package
     * @param newProduct product being propose
     * @return -1 if the courier can't delivery and the time added with that delivery otherwise
     */
    private float getDeliveryTime(Product newProduct) {
        //check if there is still capacity
        if(usedCapacity + newProduct.getVolume() > maxCapacity) return -1;

        List<Product> productsCopy = new ArrayList<>(listOfDeliveries);
        addDelivery(newProduct, productsCopy);
        float totalTime = calculateTotalTime(productsCopy);

        if(totalTime > maxWorkHoursPerDay) {
            return -1;
        }
        else {
            float result;
            if(algorithm == 1) {
                float initialTime = calculateTotalTime(listOfDeliveries);
                result = totalTime - initialTime;
            } else if( algorithm == 3){
                result = totalTime;
            } else {
                if(listOfDeliveries.size() == 0) {
                    result = maxWorkHoursPerDay + 1;
                } else {
                    result = totalTime;
                }
            }

            BigDecimal bigDecimal = new BigDecimal(result).setScale(2, RoundingMode.HALF_UP);
            return bigDecimal.floatValue();
        }
    }

    /**
     * calculates the need time to delivery all packages assign
     * @return time in hours
     * @param listOfDeliveries list of products to deliver (usually the class attribute)
     */
    private float calculateTotalTime(List<Product> listOfDeliveries) {
        float distance = 0;

        if(listOfDeliveries.size() == 0) {
            return 0;

        } else if(listOfDeliveries.size() == 1) {
            //path is delivery and came back
            distance = 2 * Location.manhattanDistance(storeLocation, listOfDeliveries.get(0).getDeliveryLocation());
        } else {
            distance += Location.manhattanDistance(storeLocation, listOfDeliveries.get(0).getDeliveryLocation());

            for (int j = 0; j < listOfDeliveries.size() - 1; j++) {
                distance += Location.manhattanDistance(listOfDeliveries.get(j).getDeliveryLocation(), listOfDeliveries.get(j + 1).getDeliveryLocation());
            }

            distance += Location.manhattanDistance(listOfDeliveries.get(listOfDeliveries.size() - 1).getDeliveryLocation(), storeLocation);
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

            float timeTillDelivery = getDeliveryTime(product);
            if(timeTillDelivery == -1) {
                System.out.println("[" + this.getAgent().getLocalName() + "] Cannot fulfill request.");
                reply.setPerformative(ACLMessage.REFUSE);
            }
            else {
                try {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(timeTillDelivery));
                    System.out.println("[" + this.getAgent().getLocalName() + "] Proposing value " + timeTillDelivery + ".");
                } catch (Exception e) {
                    System.err.println("[" + this.getAgent().getLocalName() + "] Error in sending proposal.");
                }
            }
            return reply;
        }

        protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
            return;
        }

        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
            System.out.println("[" + this.getAgent().getLocalName() + "] Confirming delivery...");

            try {
                Product product = (Product) cfp.getContentObject();
                addDelivery(product, listOfDeliveries);
            } catch (UnreadableException e) {
                System.err.println("[" + this.getAgent().getLocalName() + "] Error confirming delivery");
            }

            ACLMessage result = accept.createReply();
            result.setPerformative(ACLMessage.INFORM);
            float totalTime = calculateTotalTime(listOfDeliveries);
            try {
                result.setContentObject(totalTime * velocity); //Confirm delivery with current total distance
            } catch (IOException e) {
                System.err.println("[" + this.getAgent().getLocalName() + "] Error setting totalTime response");
            }

            return result;
        }

    }
}
