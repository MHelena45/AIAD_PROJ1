package AuxiliaryClasses.Evaluators;

import Agents.CourierAgent;
import AuxiliaryClasses.Product;

public interface IEvaluator {
    float evaluate(CourierAgent courier, Product newProduct);
}
