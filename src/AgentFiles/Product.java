package AgentFiles;

import java.io.Serializable;

public class Product implements Serializable {
    private int id;
    private Location deliveryLocation;
    private int timeToDeliver;
    private int volume;

    public Product(int id, Location deliveryLocation, int timeToDeliver, int volume) {
        this.id = id;
        this.deliveryLocation = deliveryLocation;
        this.timeToDeliver = timeToDeliver;
        this.volume = volume;
    }


    public Location getDeliveryLocation() {
        return deliveryLocation;
    }

    public int getId() {
        return id;
    }

    public int getVolume() {
        return volume;
    }
}