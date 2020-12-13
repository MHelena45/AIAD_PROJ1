package Agents;

import AuxiliaryClasses.Product;

import java.util.List;

public interface GraphicsDisplay {
    void addDeliveryNode(Product product);
    void setGreen(String nodeName);
    void pause();
    void drawEdges(List<Product> productList, String courierName);
}
