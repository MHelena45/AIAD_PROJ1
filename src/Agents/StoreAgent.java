package Agents;

import AuxiliaryClasses.Location;
import AuxiliaryClasses.Product;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class StoreAgent extends Agent {
    private final Location storeLocation = new Location(0,0);
    private List<Product> listOfOrders;
    private List<AID> couriers;
    private int expectedNumberOfCouriers;
    private volatile boolean busy = false;
    private int totalPackageNumber;
    private int rejectedPackagesNumber;
    private Hashtable<AID, CourierTuple> usedCouriers = new Hashtable<>();

    private class CourierTuple {
        private float distance;
        private int numPackages;

        public CourierTuple(float distance, int numPackages) {
            this.distance = distance;
            this.numPackages = numPackages;
        }

        public int getNumPackages() {
            return numPackages;
        }

        public float getDistance() {
            return distance;
        }

        public void addNewPackage(float distance) {
            this.distance = distance;
            this.numPackages++;
        }
    }


    public void setup() {
        Object[] args = getArguments();
        this.listOfOrders = (List<Product>) args[0];
        this.expectedNumberOfCouriers = (int) args[1];
        this.couriers = new ArrayList<>();
        this.totalPackageNumber = listOfOrders.size();
        addBehaviour(new CheckInResponder());
        System.out.println("[STORE] Setup complete");
    }

    private void addCourier(AID courierAgent) {
        this.couriers.add(courierAgent);
    }

    private void printOutput() {
        System.out.println("[OUTPUT]:");
        System.out.println("\t- Rejected " + rejectedPackagesNumber + " out of " + totalPackageNumber + " Packages");
        System.out.println("\t- Used " + usedCouriers.size() + " out of " + couriers.size() + " Couriers");
        float totalDist = 0f;
        float avgTime = 0;
        Set<AID> keys = usedCouriers.keySet();
        Iterator<AID> itr = keys.iterator();
        AID key;
        while (itr.hasNext()) {
            key = itr.next();
            totalDist += usedCouriers.get(key).getDistance();
            float time = usedCouriers.get(key).getDistance() / 40; //40 because velocity is 40km/h
            avgTime += time / usedCouriers.get(key).getNumPackages();
        }
        avgTime /= usedCouriers.size();
        System.out.println("\t- Total Distance: " + totalDist + " km");

        BigDecimal bigDecimal = new BigDecimal(avgTime).setScale(2, RoundingMode.HALF_UP);
        System.out.println("\t- Average Time To Deliver 1 Package: " + bigDecimal.floatValue() + " h");
    }

    private void sendDeliveryRequest(Product product) { //Call this when we want to send a delivery request to our Couriers
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        try {
            cfp.setContentObject(product);
        } catch (IOException e) {
            System.err.println("[STORE] Couldn't set content Object in CFP message with product: " + product.toString());
            return;
        }

        addBehaviour(new FIPAContractNetInit(this, cfp, couriers));
    }

    class CheckInResponder extends CyclicBehaviour {
        CheckInResponder() {
            System.out.println("[STORE] Waiting for check-ins...");
        }

        @Override
        public void action() {
            ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE));
            if(msg != null) {
                AID courierAgent;
                try {
                    courierAgent = (AID) msg.getContentObject();
                } catch (UnreadableException e) {
                    System.err.println("[STORE] Error parsing courier message.");
                    return;
                }
                addCourier(courierAgent);
                System.out.println("[STORE] Checked-in " + courierAgent.getLocalName() + ".");

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("Check-in received");
                send(reply);

                if(couriers.size() == expectedNumberOfCouriers) {
                    addBehaviour(new ProductBroadcaster(this.getAgent(), 100));
                }
            }
            else block();
        }
    }

    class ProductBroadcaster extends TickerBehaviour {
        private List<Product> products;

        public ProductBroadcaster(Agent a, long period) {
            super(a, period);
            this.products = ((StoreAgent) this.getAgent()).listOfOrders;
        }

        @Override
        protected void onTick() {
            if(busy) return;
            else busy = true;
            Product product = products.get(0);
            System.out.println("[STORE] Proposing new product: (size: " + product.getVolume() + ", location: <" + product.getDeliveryLocation().getX() + "," + product.getDeliveryLocation().getY() + ">)");
            sendDeliveryRequest(product);
            products.remove(0);
            if(products.isEmpty()) {
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                printOutput();
                            }
                        },
                        1000
                );
                this.stop();
            }
        }
    }

    class FIPAContractNetInit extends ContractNetInitiator {
        private List<AID> couriers;

        FIPAContractNetInit(Agent a, ACLMessage msg, List<AID> couriers) {
            super(a, msg);
            this.couriers = couriers;
        }

        protected Vector prepareCfps(ACLMessage cfp) {
            Vector<ACLMessage> v = new Vector<>();

            for(AID courierAgent : couriers) {
                cfp.addReceiver(courierAgent);
            }

            v.add(cfp);
            return v;
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {
            AID chosenAgent = null;
            float minTime = Float.MAX_VALUE;
            for (Object response : responses) {
                if(((ACLMessage) response).getPerformative() == ACLMessage.REFUSE)
                    continue;
                float timeTaken = Float.parseFloat(((ACLMessage) response).getContent());
                if(timeTaken < minTime) {
                    chosenAgent = ((ACLMessage) response).getSender();
                    minTime = timeTaken;
                }
            }

            if (chosenAgent == null) {
                rejectedPackagesNumber++;
                busy = false;
                System.out.println("[STORE] No agent available for delivery of product.");
            }
            else {
                String offersString = "[STORE] Selected agent " + chosenAgent.getLocalName() + " for product delivery, offers were: [";
                for(int i = 0; i < responses.size(); i++) {
                    ACLMessage message = ((ACLMessage) responses.get(i));
                    if(message.getPerformative() == ACLMessage.REFUSE)
                        continue;
                    float timeTaken = Float.parseFloat(message.getContent());
                    offersString += message.getSender().getLocalName() + ": " + timeTaken;
                    if (i != responses.size() - 1)
                        offersString += "; ";
                }
                offersString += "]";
                System.out.println(offersString);
            }

            for (Object response : responses) {
                ACLMessage msg = ((ACLMessage) response).createReply();
                if(((ACLMessage) response).getSender() == chosenAgent) {
                    try {
                        msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    } catch (Exception e) {
                        System.err.println("[STORE] Unable to send proposal acceptance.");
                    }
                } else {
                    msg.setPerformative(ACLMessage.REJECT_PROPOSAL);
                }
                acceptances.add(msg);
            }
        }

        protected void handleAllResultNotifications(Vector resultNotifications) {
            System.out.println("[STORE] Agent " + ((ACLMessage)resultNotifications.get(0)).getSender().getLocalName() + " confirmed product delivery.");
            float totalDist = 0f;
            AID sender = ((ACLMessage)resultNotifications.get(0)).getSender();
            try {
                totalDist = (float) ((ACLMessage)resultNotifications.get(0)).getContentObject();
            } catch (UnreadableException e) {
                System.err.println("[STORE] Unable to red courier total time.");
            }
            if(usedCouriers.get(sender) == null) {
                CourierTuple tuple = new CourierTuple(totalDist,1);
                usedCouriers.put(sender, tuple);
            }
            else {
                CourierTuple tuple = usedCouriers.get(sender);
                tuple.addNewPackage(totalDist);
            }
            busy = false;
        }

    }
}
