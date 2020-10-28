package AgentFiles;

import java.io.Serializable;

public class Product implements Serializable {
    private Location deliveryLocation;
    private int maxTimeToDeliver; //TODO change this into a timestamp format? We need to decide a way to track time

    public Product(Location deliveryLocation, int timeToDeliver) {
        this.deliveryLocation = deliveryLocation;
        this.maxTimeToDeliver = timeToDeliver;
    }

    public Location getDeliveryLocation() {
        return deliveryLocation;
    }
}
