package AuxiliaryClasses.Evaluators;

import Agents.CourierAgent;
import AuxiliaryClasses.AlgorithmUsed;
import AuxiliaryClasses.Location;
import AuxiliaryClasses.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class MinimumCouriersEvaluator implements IEvaluator {
    /**
     * checks if the courier can accept the package
     * @param newProduct product being propose
     * @return -1 if the courier can't delivery and the time added with that delivery otherwise
     */
    public float evaluate(CourierAgent courier, Product newProduct) {
        //check if there is still capacity
        if(courier.usedCapacity + newProduct.getVolume() > courier.getMaxCapacity()) return -1;

        List<Product> productsCopy = new ArrayList<>(courier.listOfDeliveries);
        courier.addDelivery(newProduct, productsCopy);
        float initialTime = Location.calculateTotalTime(courier.getStoreLocation(), courier, courier.listOfDeliveries);
        float totalTime = Location.calculateTotalTime(courier.getStoreLocation(), courier, productsCopy);

        if(totalTime > courier.getMaxWorkHoursPerDay()) {
            return -1;
        }
        else {
            float result;
            if(courier.listOfDeliveries.size() == 0) {
                result = courier.getMaxWorkHoursPerDay() + 1;
            } else {
                result = totalTime - initialTime;
            }
            return result;
        }
    }
}
