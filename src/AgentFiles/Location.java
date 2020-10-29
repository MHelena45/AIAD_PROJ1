package AgentFiles;

import java.io.Serializable;

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
}
