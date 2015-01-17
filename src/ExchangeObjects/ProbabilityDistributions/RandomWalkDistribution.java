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
public class RandomWalkDistribution extends ProbabilityDistribution {

    private final Random random = new Random();
    private double location;
    private final double volatility;

    public RandomWalkDistribution(double location, double volatility) {
        this.location = location;
        this.volatility = volatility;
    }

    @Override
    public double nextValue() {
        location = Math.max(-1, Math.min(1, location + volatility * 2 * random.nextDouble() - volatility));
        return location;
    }

    public RandomWalkDistribution() {
        this.location = 0;
        this.volatility = .001;
    }
}
