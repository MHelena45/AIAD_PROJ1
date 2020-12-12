package AuxiliaryClasses;

import java.io.Serializable;
import java.util.List;

public class ConfirmationResult implements Serializable {
    private Product product;
    private float totalDistance;
    private List<Product> productList;

    public ConfirmationResult(Product product, float totalDistance, List<Product> productList) {
        this.product = product;
        this.totalDistance = totalDistance;
        this.productList = productList;
    }

    public List<Product> getProductList() {
        return productList;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public Product getProduct() {
        return product;
    }
}
