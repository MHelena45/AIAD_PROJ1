package Messages;

import AgentFiles.Product;

import java.util.List;

public class TotalFinalTimeOfTrajectory {
    
    private int version, totalFinalTimeOfTraject;
    private MessageType type;
    private List<Product> orders;


    public TotalFinalTimeOfTrajectory(int version, List<Product> order) {
        this.version = version;
        this.type = MessageType.TotalFinalTimeOfTraject;
        this.orders = order;
    }

}