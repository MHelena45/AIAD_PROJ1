public class Product {
    private Location deliveryLocation;
    private int timeToDeliver; //TODO change this into a timestamp format? We need to decide a way to track time

    public Product(Location deliveryLocation, int timeToDeliver) {
        this.deliveryLocation = deliveryLocation;
        this.timeToDeliver = timeToDeliver;
    }
}
