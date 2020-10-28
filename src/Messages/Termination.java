package Messages;

import AgentFiles.Product;

public class Termination{
    
    private int version;
    private MessageType type;

    /* This message informs the courier that he can start delivering */
    public Termination(int version, Product order) {
        this.version = version;
        this.type = MessageType.Termination;
    }

}
