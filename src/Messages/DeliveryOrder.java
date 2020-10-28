package Messages;

import AgentFiles.Product;

public class DeliveryOrder {
    
    private int version;
    private MessageType type;
    private Product order;

    public DeliveryOrder(int version, Product order) {
        this.version = version;
        this.type = MessageType.DeliveryOrder;
        this.order = order;
    }

}
