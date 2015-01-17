/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ExchangeObjects.ProbabilityDistributions;

import ExchangeObjects.ProbabilityDistribution;
import java.util.List;

/**
 *
 * @author Sasa
 */
public class CyclicValueDistribution extends ProbabilityDistribution {
    private final List<Double> values;
    private int index = -1;

    public CyclicValueDistribution(List<Double> values) {
        this.values = values;
    }
    
    @Override
    public double nextValue() {
        index = (index + 1) % values.size();
        return values.get(index);
    }
    
}
