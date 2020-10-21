public class DeliveryOrder {
    private int version;
    private int type;
    private Product order;

    public DeliveryOrder(int version, int type, Product order) {
        this.version = version;
        this.type = type;
        this.order = order;
    }

}
