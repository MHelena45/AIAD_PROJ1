package Messages;

import AgentFiles.Product;

public class Assignment {
    
    private int version;
    private MessageType type;
    private Product order;

    public Assignment(int version, Product order) {
        this.version = version;
        this.type = MessageType.Assignment;
        this.order = order;
    }

}
