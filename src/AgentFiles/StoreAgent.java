package AgentFiles;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class StoreAgent extends Agent {
    private final Location storeLocation = new Location(0,0);
    public List<Product> listOfOrders;
    private List<AID> couriers;

    public void setup() {
        Object[] args = getArguments();
        this.listOfOrders = (List<Product>) args[0];
        this.couriers = new ArrayList<>();
        addBehaviour(new CheckInResponder());
        System.out.println("[STORE] Setup complete");
    }

    private void addCourier(AID courierAgent) {
        this.couriers.add(courierAgent);
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

                if(couriers.size() == 5) {
                    addBehaviour(new ProductBroadcaster(this.getAgent(), 2000));
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
            System.out.println("[STORE] Proposing new product...");
            sendDeliveryRequest(products.get(0));
            products.remove(0);
            if(products.isEmpty())
                this.stop();
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

            if (chosenAgent == null)
                System.out.println("[STORE] No agent available for delivery of product.");
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
        }

    }
}
