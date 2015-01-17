/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ExchangeObjects;

/**
 *
 * @author Sasa
 */
public enum OrderType {

    BID, ASK;

    public OrderType other() {
        return this.equals(BID) ? ASK : BID;
    }
}
