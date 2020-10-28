package Mensages;

import AgentFiles.Product;

import java.util.List;

public class TotalFinalTimeOfTraject{
    
    private int version, totalFinalTimeOfTraject;
    private MessageType type;
    private List<Product> orders;


    public TotalFinalTimeOfTraject(int version, List<Product> order) {
        this.version = version;
        this.type = MessageType.TotalFinalTimeOfTraject;
        this.orders = order;
    }

}