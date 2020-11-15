package AuxiliaryClasses;

import Agents.CourierAgent;

import java.io.Serializable;
import java.util.List;

public class Location implements Serializable {
    private int x;
    private int y;

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    //Create functions related to finding distances between locations etc.
   /* public double calculateDistance(Location destination) {
        return Math.sqrt(Math.pow(this.y - destination.y, 2) + Math.pow(this.x - destination.x, 2));
    } */

    //functions related to finding distances between locations etc.
    public static int manhattanDistance(Location L1, Location L2) {
        return Math.abs(L1.x - L2.x) + Math.abs(L1.y - L2.y);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public static float calculateTotalTime(Location storeLocation, CourierAgent courier, List<Product> listOfDeliveries) {
        float distance = 0;

        if(listOfDeliveries.size() == 0) {
            return 0;

        } else if(listOfDeliveries.size() == 1) {
            //path is delivery and came back
            distance = 2 * Location.manhattanDistance(storeLocation, listOfDeliveries.get(0).getDeliveryLocation());
        } else {
            distance += Location.manhattanDistance(storeLocation, listOfDeliveries.get(0).getDeliveryLocation());

            for (int j = 0; j < listOfDeliveries.size() - 1; j++) {
                distance += Location.manhattanDistance(listOfDeliveries.get(j).getDeliveryLocation(), listOfDeliveries.get(j + 1).getDeliveryLocation());
            }

            distance += Location.manhattanDistance(listOfDeliveries.get(listOfDeliveries.size() - 1).getDeliveryLocation(), storeLocation);
        }

        return distance/courier.getVelocity();
    }
}
