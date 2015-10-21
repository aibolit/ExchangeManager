/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ExchangeObjects;

import java.util.Objects;

/**
 *
 * @author Sasa
 */
public class Security {

    private final String ticker;
    private final double dividend;
    private final double volatility;
    private double netWorth;
    private final ProbabilityDistribution outlook;
    private final int totalShares;

    public Security(String ticker, int totalShares, double dividend, double volatility, double netWorth, ProbabilityDistribution outlook) {
        this.ticker = ticker;
        this.dividend = dividend;
        this.volatility = volatility;
        this.outlook = outlook;
        this.totalShares = totalShares;
        this.netWorth = netWorth;
    }

    public String getTicker() {
        return ticker;
    }

    public double getDividend() {
        return dividend;
    }

    public double getVolatility() {
        return volatility;
    }

    public double getNetWorth() {
        return netWorth;
    }

    public int getTotalShares() {
        return totalShares;
    }

    public double dividendPayout() {
        return netWorth * dividend / totalShares;
    }

    public double nextRound() {
        return netWorth += netWorth * outlook.nextValue() * volatility;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.ticker);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Security other = (Security) obj;
        return Objects.equals(this.ticker, other.getTicker());
    }

    @Override
    public String toString() {
        return "Security{" + "ticker=" + ticker + ", dividend=" + dividend + ", volatility=" + volatility + ", netWorth=" + netWorth + ", outlook=" + outlook + ", totalShares=" + totalShares + '}';
    }

}
