public class EstimatedTime {
    
    private int version, estimatedTime;
    private MessageType type;
    private Product order;


    public EstimatedTime(int version, Product order) {
        this.version = version;
        this.type = EstimatedTime;
        this.order = order;
    }

}
