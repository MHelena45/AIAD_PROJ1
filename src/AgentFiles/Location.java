package AgentFiles;

public class Location {
    private int x;
    private int y;

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    //Create functions related to finding distances between locations etc.
    public double calculateDistance(Location destination) {
        return Math.sqrt(Math.pow(this.y - destination.y, 2) + Math.pow(this.x - destination.x, 2));
    }
}
