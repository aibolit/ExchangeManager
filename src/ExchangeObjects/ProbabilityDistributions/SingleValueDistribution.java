/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ExchangeObjects.ProbabilityDistributions;

import ExchangeObjects.ProbabilityDistribution;

/**
 *
 * @author Sasa
 */
public class SingleValueDistribution extends ProbabilityDistribution {
    private final double value;

    public SingleValueDistribution(double value) {
        this.value = value;
    }

    @Override
    public double nextValue() {
        return value;
    }
}
