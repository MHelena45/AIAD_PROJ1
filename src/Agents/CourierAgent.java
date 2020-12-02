package Agents;

import AuxiliaryClasses.AlgorithmUsed;
import AuxiliaryClasses.Evaluators.IEvaluator;
import AuxiliaryClasses.Evaluators.IncrementDistanceEvaluator;
import AuxiliaryClasses.Evaluators.MinimumCouriersEvaluator;
import AuxiliaryClasses.Evaluators.TotalDistanceEvaluator;
import AuxiliaryClasses.Location;
import AuxiliaryClasses.Product;
import jade.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;
import sajas.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import sajas.proto.ContractNetResponder;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CourierAgent extends Agent implements Serializable {
    private final int velocity = 40; //velocity Km/h
    private int maxWorkHoursPerDay;
    private AlgorithmUsed algorithm;
    private int maxCapacity;
    private Location storeLocation; //start and end of the trajectory
    private AID storeAID;
    private IEvaluator evaluator;

    public List<Product> listOfDeliveries;
    public int usedCapacity = 0;

    @Override
    public void setup() {
        Object[] args = getArguments();
        maxWorkHoursPerDay = (int) args[0];
        storeLocation = (Location) args[1];
        maxCapacity = (int) args[2];
        algorithm = (AlgorithmUsed) args[3];
        switch(algorithm) {
            case MinimizeDistance:
                evaluator = new IncrementDistanceEvaluator();
                break;
            case MinimizeTimeToDelivery:
                evaluator = new TotalDistanceEvaluator();
                break;
            case MinimizeCars:
                evaluator = new MinimumCouriersEvaluator();
                break;
        }

        System.out.println("[" + this.getLocalName() + "] Courier created, with capacity " + maxCapacity + ".");

        listOfDeliveries = new ArrayList<>();

        addBehaviour(new CourierCheckIn(this.getAID(), this)); //Check in to the store
        addBehaviour(new FIPAContractNetResp(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));
    }

    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    public float getVelocity() {
        return this.velocity;
    }

    public Location getStoreLocation() {
        return storeLocation;
    }

    public float getMaxWorkHoursPerDay() {
        return maxWorkHoursPerDay;
    }

    /**
     * Adds delivery in the correct place
     * @param product product being
     * @param listOfDeliveries list where product should be added (usually class attribute)
     */
    public void addDelivery(Product product, List<Product> listOfDeliveries) {
        int finalPosition = 0;
        float distance = -1;
        for (int i = 0; i < listOfDeliveries.size(); i++) {
            List<Product> productsCopy = new ArrayList<>(listOfDeliveries);
            productsCopy.add(i, product);
            float tmpDistance = Location.calculateTotalTime(storeLocation, this, productsCopy);

            //check if it was instantiated or current value is smaller
            if (distance == -1 || distance > tmpDistance) {
                distance = tmpDistance;
                finalPosition = i;
            }
        }

        listOfDeliveries.add(finalPosition, product);

        if(listOfDeliveries.equals(this.listOfDeliveries)) this.usedCapacity += product.getVolume(); //only if we're adding to the class list
    }

    class CourierCheckIn extends Behaviour {
        boolean checkedIn = false;
        private AID courierAID;
        private Agent thisAgent;

        CourierCheckIn(AID courierAID, Agent thisAgent) {
            this.courierAID = courierAID;
            this.thisAgent = thisAgent;
        }

        @Override
        public void action() {
            DFAgentDescription df = new DFAgentDescription();
            ServiceDescription sd  = new ServiceDescription();
            sd.setType( "store" );
            df.addServices(sd);
            DFAgentDescription[] result = null;
            boolean foundStore = false;
            final int maxTries = 10;
            int currTries = 0;
            while(!foundStore) {
                try {
                    result = DFService.search(thisAgent, df);
                    if(result.length < 1) {
                        System.out.println("[" + getLocalName() + "] Couldn't find store agent in Yellow Pages");
                        currTries++;
                        Thread.sleep(100);
                    } else foundStore = true;

                    if(currTries == maxTries) {
                        System.err.println("[" + getLocalName() + "] Exceeded Max Tries in Store Check-In. Terminating Agent");
                        return;
                    }
                } catch (FIPAException e) {
                    System.err.println("[" + getLocalName() + "] Error trying to find store agent in Yellow Pages");
                    return;
                } catch (InterruptedException e) {
                    System.err.println("[" + getLocalName() + "] Couldn't Sleep");
                    return;
                }
            }

            storeAID = result[0].getName();

            ACLMessage msg = new ACLMessage(ACLMessage.SUBSCRIBE);
            try {
                System.out.println("[" + courierAID.getLocalName() + "] Checking in...");
                msg.setContentObject(courierAID);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("[" + courierAID.getLocalName() + "] Couldn't send check-In message with Courier: " + this);
                checkedIn = true;
                return;
            }
            msg.addReceiver(storeAID);
            send(msg);

            checkedIn = true;
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

        @Override
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

            float timeTillDelivery = evaluator.evaluate((CourierAgent) this.getAgent(), product);
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
            float totalTime = Location.calculateTotalTime(storeLocation, (CourierAgent) this.getAgent(), listOfDeliveries);
            try {
                result.setContentObject(totalTime * velocity); //Confirm delivery with current total distance
            } catch (IOException e) {
                System.err.println("[" + this.getAgent().getLocalName() + "] Error setting totalTime response");
            }

            return result;
        }

    }
}
