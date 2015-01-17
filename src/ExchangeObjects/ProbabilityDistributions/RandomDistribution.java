/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ExchangeObjects.ProbabilityDistributions;

import ExchangeObjects.ProbabilityDistribution;
import java.util.Random;

/**
 *
 * @author Sasa
 */
public class RandomDistribution extends ProbabilityDistribution{
    private final Random random = new Random();
    private final double low;
    private final double high;

    public RandomDistribution() {
        this.low = -1;
        this.high = 1;
    }

    public RandomDistribution(double low, double high) {
        this.low = low;
        this.high = high;
    }

    
    
    @Override
    public double nextValue() {
        return low + (random.nextDouble() * (high - low));
    }
    
    
}
