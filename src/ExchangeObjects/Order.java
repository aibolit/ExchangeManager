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
public class Order implements Comparable<Order> {
    
    private final String user;
    private final Security security;
    private final int shares;
    private final OrderType type;
    private final double price;
    private final long timestamp;
    
    public Order(String user, Security ticker, int shares, OrderType type, double price) throws ExchangeException {
        if (price <= 0) {
            throw new ExchangeException("Price must be a positive number");
        }
        this.user = user;
        this.security = ticker;
        this.shares = shares;
        this.type = type;
        this.price = price;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Order(Order other, int shares) {
        this.user = other.getUser();
        this.security = other.getSecurity();
        this.shares = shares;
        this.type = other.getType();
        this.price = other.getPrice();
        this.timestamp = other.getTimestamp();
    }
    
    public String getUser() {
        return user;
    }
    
    public Security getSecurity() {
        return security;
    }
    
    public int getShares() {
        return shares;
    }
    
    public OrderType getType() {
        return type;
    }
    
    public double getPrice() {
        return price;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "Order{" + "user=" + user + ", security=" + security.getTicker() + ", shares=" + shares + ", type=" + type + ", price=" + price + '}';
    }
    
    @Override
    public int compareTo(Order t) {
        if (this.getSecurity() == null ? t.getSecurity() != null : !this.getSecurity().equals(t.getSecurity())) {
            throw new IllegalArgumentException("Cannot compare order for ticker=" + this.getSecurity() + " to other order ticker=" + t.getSecurity());
        }
        if (this.getType() != t.getType()) {
            throw new IllegalArgumentException("Order Types do not match");
        }
        double rval = this.getType() == OrderType.BID ? this.getPrice() - t.getPrice() : t.getPrice() - this.getPrice();
        rval = rval != 0 ? rval : this.timestamp - t.getTimestamp();
        return rval == 0 ? 0 : rval > 0 ? -1 : 1;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.user);
        hash = 97 * hash + Objects.hashCode(this.security);
        hash = 97 * hash + Objects.hashCode(this.type);
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Order other = (Order) obj;
        if (!Objects.equals(this.user, other.getUser())) return false;
        if (!Objects.equals(this.security, other.getSecurity())) return false;
        return this.type == other.getType();
    }
}
