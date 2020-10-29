package AgentFiles;

import java.io.Serializable;

public class Product implements Serializable {
    private int id;
    private Location deliveryLocation;
    private int timeToDeliver; //TODO change this into a timestamp format? We need to decide a way to track time

    public Product(int id, Location deliveryLocation, int timeToDeliver) {
        this.id = id;
        this.deliveryLocation = deliveryLocation;
        this.timeToDeliver = timeToDeliver;
    }


    public Location getDeliveryLocation() {
        return deliveryLocation;
    }

    public int getId() {
        return id;
    }
}