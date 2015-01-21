/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ExchangeObjects;

import exchangemanager.Configurations;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sasa
 */
public class Exchange implements Runnable {
    private final Map<String, Map<Security, Integer>> userShares = new ConcurrentHashMap<>();
    private final Map<String, Map<Security, Double>> userDivdendFactors = new ConcurrentHashMap<>();
    private final Map<String, Double> userCash = new ConcurrentHashMap<>();
    private final Map<Security, SortedSet<Order>> bidOrders = new ConcurrentHashMap<>();
    private final Map<Security, SortedSet<Order>> askOrders = new ConcurrentHashMap<>();
    private final Map<OrderType, Map<Security, SortedSet<Order>>> orders = new EnumMap<>(OrderType.class);
    private final Map<String, Security> securities = new ConcurrentHashMap<>();
    private final Map<Security, Double> securityValuations = new ConcurrentHashMap<>();
    private final Map<Security, Double> exchangeValuations = new ConcurrentHashMap<>();
    private final Map<String, List<String>> userUpdates = new ConcurrentHashMap<>();
    private volatile boolean isRunning = true;

    public Exchange() throws ExchangeException {
        orders.put(OrderType.BID, bidOrders);
        orders.put(OrderType.ASK, askOrders);
        for (String user : Configurations.getUsers()) {
            userShares.put(user, new ConcurrentHashMap<Security, Integer>());
            userDivdendFactors.put(user, new ConcurrentHashMap<Security, Double>());
            for (Security security : Configurations.getSecurities()) {
                userShares.get(user).put(security, 0);
                userDivdendFactors.get(user).put(security, 0.0);
            }
            userCash.put(user, Configurations.getInitialCash());
        }
        for (Security security : Configurations.getSecurities()) {
            double initialPrice = security.getNetWorth() * security.getDividend() / security.getTotalShares() * Configurations.getExchangePriceMultiplier();
            userShares.get(Configurations.EXCHANGE_USER).put(security, security.getTotalShares());
            securities.put(security.getTicker(), security);
            bidOrders.put(security, new ConcurrentSkipListSet<Order>());
            askOrders.put(security, new ConcurrentSkipListSet<Order>());
            askOrders.get(security).add(new Order(
                Configurations.EXCHANGE_USER,
                security,
                security.getTotalShares(),
                OrderType.ASK,
                initialPrice * Configurations.getExchangeBuyFactor()
            ));
            bidOrders.get(security).add(new Order(
                Configurations.EXCHANGE_USER,
                security,
                security.getTotalShares(),
                OrderType.BID,
                initialPrice * Configurations.getExchangeSellFactor()
            ));
            securityValuations.put(security, initialPrice);
            exchangeValuations.put(security, initialPrice);
        }
        userCash.put(Configurations.EXCHANGE_USER, Double.POSITIVE_INFINITY);
    }

    public synchronized boolean isRun() {
        return isRunning;
    }

    public synchronized void setRun(boolean run) {
        this.isRunning = run;
    }

    private synchronized void addUserUpdates(Map<String, List<String>> localUserUpdates) {
        for (Map.Entry<String, List<String>> entry : localUserUpdates.entrySet()) {
            String user = entry.getKey();
            List<String> updates = entry.getValue();
            if (userUpdates.containsKey(user)) {
                userUpdates.get(user).addAll(updates);
            } else {
                userUpdates.put(user, updates);
            }
        }
        this.notifyAll();
    }

    private synchronized void nextRound() {
        if(!isRunning) return;
        for (Map.Entry<String, Map<Security, Integer>> userSharesEntry : userShares.entrySet()) {
            String user = userSharesEntry.getKey();
            for (Map.Entry<Security, Integer> entry : userSharesEntry.getValue().entrySet()) {
                Security security = entry.getKey();
                int shares = entry.getValue();
                userCash.put(user, userCash.get(user) + entry.getKey().dividendPayout() * shares * userDivdendFactors.get(user).get(security));
            }
        }
        for (Map<Security, Double> entry : userDivdendFactors.values()) {
            for (Security security : entry.keySet()) {
                entry.put(security, entry.get(security) * Configurations.getDividendGrowthFactor());
            }
        }
        for (Security security : Configurations.getSecurities()) {
            for (OrderType orderType : OrderType.values()) {
                try {
                    removeOrder(Configurations.EXCHANGE_USER, security.getTicker(), orderType);
                } catch (ExchangeException ex) {
                    ex.printStackTrace();
                }
            }
            double newValuation
                = Configurations.getExchangeValuationAlpha() * securityValuations.get(security)
                + (1 - Configurations.getExchangeValuationAlpha()) * exchangeValuations.get(security);
            exchangeValuations.put(security, newValuation);

            int exchangeShares = userShares.get(Configurations.EXCHANGE_USER).get(security);
            if (exchangeShares > 0) {
                try {
                    Map<String, List<String>> localUserUpdates = newOrder(new Order(
                        Configurations.EXCHANGE_USER,
                        security,
                        exchangeShares,
                        OrderType.ASK,
                        newValuation * Configurations.getExchangeBuyFactor()
                    ));
                    addUserUpdates(localUserUpdates);
                } catch (ExchangeException ex) {
                    ex.printStackTrace();
                }
            }
            try {
                Map<String, List<String>> localUserUpdates = newOrder(new Order(
                    Configurations.EXCHANGE_USER,
                    security,
                    security.getTotalShares(),
                    OrderType.BID,
                    newValuation * Configurations.getExchangeSellFactor()
                ));
                addUserUpdates(localUserUpdates);
            } catch (ExchangeException ex) {
                Logger.getLogger(Exchange.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        for (Security security : Configurations.getSecurities()) {
            security.nextRound();
        }
    }

    public synchronized Map<OrderType, SortedSet<Order>> getOrderStatus(String ticker) throws ExchangeException {
        Security security;
        if ((security = securities.get(ticker)) == null) {
            throw new ExchangeException("Invalid ticker");
        }
        Map<OrderType, SortedSet<Order>> ret = new EnumMap<>(OrderType.class);

        for (Map.Entry<OrderType, Map<Security, SortedSet<Order>>> orderTypeEntry : orders.entrySet()) {
            int sharesLeft = (int) (security.getTotalShares() * Configurations.getVisibleShares());
            OrderType orderType = orderTypeEntry.getKey();
            ConcurrentSkipListSet<Order> typeRet = new ConcurrentSkipListSet<>();
            SortedSet<Order> orderQueue = orderTypeEntry.getValue().get(security);
            for (Order order : orderQueue) {
                int shares = Math.min(sharesLeft, order.getShares());
                typeRet.add(new Order(order, shares));
                sharesLeft -= shares;
                if (sharesLeft <= 0) {
                    break;
                }
            }
            ret.put(orderType, typeRet);
        }
        return ret;
    }

    public synchronized double getUserCash(String user) throws ExchangeException {
        return userCash.get(user);
    }

    public synchronized Map<Security, Integer> getUserShares(String user) {
        return Collections.unmodifiableMap(userShares.get(user));
    }

    public synchronized Map<Security, Double> getUserDividendFactors(String user) {
        return Collections.unmodifiableMap(userDivdendFactors.get(user));
    }

    public synchronized double getUserValue(String user) {
        double cash = userCash.get(user);
        for (Map.Entry<Security, Integer> entry : userShares.get(user).entrySet()) {
            Security security = entry.getKey();
            Integer shares = entry.getValue();
            cash += shares * securityValuations.get(security);
        }
        return cash;
    }

    public synchronized Set<Order> getUserOrders(String user) {
        Set<Order> ret = new CopyOnWriteArraySet<>();
        for (Map.Entry<OrderType, Map<Security, SortedSet<Order>>> orderTypeEntry : orders.entrySet()) {
            for (Map.Entry<Security, SortedSet<Order>> secMap : orderTypeEntry.getValue().entrySet()) {
                for (Order order : secMap.getValue()) {
                    if (order.getUser().equals(user)) {
                        ret.add(order);
                    }
                }
            }

        }
        return ret;
    }

    private synchronized void addSubscriptionUpdate(Map<String, List<String>> data, String user, String update) {
        if (!data.containsKey(user)) {
            data.put(user, new CopyOnWriteArrayList<String>());
        }
        data.get(user).add(update);
    }

    public synchronized Map<String, List<String>> newOrder(Order order) throws ExchangeException {
        if (order.getType() == OrderType.ASK) {
            return askOrder(order);
        } else {
            return bidOrder(order);
        }
    }

    private synchronized void changeUserShares(String user, Security security, int deltaShares) {
        int shares = userShares.get(user).get(security);
        if (shares == 0 && shares + deltaShares != 0) {
            userDivdendFactors.get(user).put(security, 1.0);
        } else if (shares != 0 && shares + deltaShares == 0) {
            userDivdendFactors.get(user).put(security, 0.0);
        }
        userShares.get(user).put(security, shares + deltaShares);
    }

    private synchronized Map<String, List<String>> askOrder(Order askOrder) throws ExchangeException {
        Security security = askOrder.getSecurity();
        String user = askOrder.getUser();
        Integer shares = userShares.get(user).get(security);
        double cash = userCash.get(user);

        //Check if this is a valid order
        if (shares == null || shares < askOrder.getShares()) {
            throw new ExchangeException("Not Enough Shares Owned");
        }

        Map<String, List<String>> ret = new ConcurrentHashMap<>();

        //Its okay, so we can remove the existing order
        for (Iterator<Order> it = askOrders.get(security).iterator(); it.hasNext();) {
            Order otherAskOrder = it.next();
            if (otherAskOrder.getUser().equals(user)) {
                it.remove();
            }
        }

        //now see if we can find any matches
        int sharesLeft = askOrder.getShares();
        Order newBidOrder = null;

        for (Iterator<Order> it = bidOrders.get(security).iterator(); it.hasNext() && sharesLeft > 0;) {
            Order bidOrder = it.next();
            if (bidOrder.getUser().equals(user)) {
                continue;
            }
            if (bidOrder.getPrice() >= askOrder.getPrice()) {
                int tradeShares = Math.min(sharesLeft, bidOrder.getShares());
                double tradePrice = (bidOrder.getPrice() + askOrder.getPrice()) / 2;
                double tradeCash = tradePrice * tradeShares;
                String bidUser = bidOrder.getUser();
                if (bidOrder.getShares() - tradeShares > 0) {
                    newBidOrder = new Order(bidOrder, bidOrder.getShares() - tradeShares);
                }
                changeUserShares(bidUser, security, tradeShares);
                //userShares.get(bidUser).put(security, tradeShares + userShares.get(bidUser).get(security));

                //Add trade to transaction return object
                addSubscriptionUpdate(ret, bidUser, "BUY " + security.getTicker() + " " + tradePrice + " " + tradeShares);
                addSubscriptionUpdate(ret, user, "SELL " + security.getTicker() + " " + tradePrice + " " + tradeShares);

                //Update cash balances
                cash += tradeCash;
                //Update bid user cash left
                double bidUserCash = userCash.get(bidUser) - tradeCash;
                userCash.put(bidUser, bidUserCash);

                //update shares left
                sharesLeft -= tradeShares;

                //remove this matching bid
                it.remove();

                //double check bid user has enough cash
                removeUserBids(bidUser, bidUserCash, security, ret);

                //update security values
                updateValuation(security, tradePrice, tradeShares);
            } else {
                break;
            }
        }
        //update my shares
        changeUserShares(user, security, sharesLeft - askOrder.getShares());
        userCash.put(user, cash);

        //update bidder leftover shares
        if (newBidOrder != null) {
            bidOrders.get(security).add(newBidOrder);
        }

        //update orders 
        if (sharesLeft > 0) {
            askOrders.get(security).add(new Order(askOrder, sharesLeft));
        }
        return ret;

    }

    private synchronized Map<String, List<String>> bidOrder(Order bidOrder) throws ExchangeException {
        Security security = bidOrder.getSecurity();
        String user = bidOrder.getUser();
        double cash = userCash.get(user);

        int sharesLeft = bidOrder.getShares();

        if (sharesLeft * bidOrder.getPrice() > cash) {
            throw new ExchangeException("Not enouch cash to make bid order.");
        }

        //Remove any existing order for this user
        for (Iterator<Order> it = bidOrders.get(security).iterator(); it.hasNext();) {
            Order otherBidOrder = it.next();
            if (otherBidOrder.getUser().equals(user)) {
                it.remove();
            }
        }
        Map<String, List<String>> ret = new ConcurrentHashMap<>();

        Order newAskOrder = null;
        for (Iterator<Order> it = askOrders.get(security).iterator(); it.hasNext() && sharesLeft > 0;) {
            Order askOrder = it.next();
            if (askOrder.getUser().equals(user)) {
                continue;
            }
            if (bidOrder.getPrice() >= askOrder.getPrice()) {
                int tradeShares = Math.min(sharesLeft, askOrder.getShares());
                double tradePrice = (bidOrder.getPrice() + askOrder.getPrice()) / 2;
                double tradeCash = tradePrice * tradeShares;
                String askUser = askOrder.getUser();
                if (askOrder.getShares() - tradeShares > 0) {
                    newAskOrder = new Order(askOrder, askOrder.getShares() - tradeShares);
                }
                changeUserShares(askUser, security, -tradeShares);
                //userShares.get(askUser).put(security, userShares.get(askUser).get(security) - tradeShares);

                //Add trade to transaction return object
                addSubscriptionUpdate(ret, user, "BUY " + security.getTicker() + " " + tradePrice + " " + tradeShares);
                addSubscriptionUpdate(ret, askUser, "SELL " + security.getTicker() + " " + tradePrice + " " + tradeShares);

                //Update cash balances
                cash -= tradeCash;
                //Update bid user cash left
                double askUserCash = userCash.get(askUser) + tradeCash;
                userCash.put(askUser, askUserCash);

                //update shares left
                sharesLeft -= tradeShares;

                //remove this matching bid
                it.remove();

                //update security values
                updateValuation(security, tradePrice, tradeShares);
            } else {
                break;
            }
        }
        //update my shares
        changeUserShares(user, security, bidOrder.getShares() - sharesLeft);
        userCash.put(user, cash);

        //update bidder leftover shares
        if (newAskOrder != null) {
            askOrders.get(security).add(newAskOrder);
        }

        //update orders 
        if (sharesLeft > 0) {
            bidOrders.get(security).add(new Order(bidOrder, sharesLeft));
        }

        //Clean up anythin Bidder cannot afford
        removeUserBids(user, cash, security, ret);

        return ret;
    }

    public synchronized void removeOrder(String user, String ticker, OrderType type) throws ExchangeException {
        Security security;
        if ((security = securities.get(ticker)) == null) {
            throw new ExchangeException("No such security");
        }

        for (Iterator<Order> it = orders.get(type).get(security).iterator(); it.hasNext();) {
            Order order = it.next();
            if (order.getUser().equals(user)) {
                it.remove();
            }
        }
    }

    private synchronized void removeUserBids(String user, double cash, Security avoid, Map<String, List<String>> ret) {
        for (Map.Entry<Security, SortedSet<Order>> bidOrdersEntry : bidOrders.entrySet()) {
            if (avoid != null && bidOrdersEntry.getKey().equals(avoid)) {
                continue;
            }
            for (Iterator<Order> it = bidOrdersEntry.getValue().iterator(); it.hasNext();) {
                Order bidOrder = it.next();
                if (bidOrder.getUser().equals(user) && bidOrder.getPrice() * bidOrder.getShares() > cash) {
                    addSubscriptionUpdate(ret, user, "REMOVE_ORDER " + bidOrder.getType() + " " + bidOrder.getSecurity().getTicker());
                    it.remove();
                }
            }
        }
    }

    private synchronized void updateValuation(Security security, double price, int shares) {
        double currentPrice = securityValuations.get(security);
        double sharesRatio = Math.min(1.0, 1.0 * shares / (security.getTotalShares() * Configurations.getPriceValuationTargetShares()));
        double newValue
            = Configurations.getPriceValuationAlpha() * (sharesRatio) * price
            + Configurations.getPriceValuationAlpha() * (1.0 - sharesRatio) * currentPrice
            + (1.0 - Configurations.getPriceValuationAlpha()) * currentPrice;
        securityValuations.put(security, newValue);
    }

    public synchronized Order createOrder(String user, OrderType type, String ticker, int shares, double price) throws ExchangeException {
        Security security;
        if ((security = securities.get(ticker)) == null) {
            throw new ExchangeException("No such security");
        }
        if (price <= 0 || shares <= 0) {
            throw new ExchangeException("Price and Shares must be positive");
        }
        return new Order(user, security, shares, type, price);
    }

    public void clearUserUpdates() {
        userUpdates.clear();
    }

    public Map<String, List<String>> getUserUpdates() {
        return Collections.unmodifiableMap(userUpdates);
    }

    public Map<Security, Double> getSecurityValuations() {
        return securityValuations;
    }
    
    @Override
    public synchronized String toString() {
        return "Exchange{" + "userShares=" + userShares + ", userCash=" + userCash + ", bidOrders=" + bidOrders + ", askOrders=" + askOrders + ", securities=" + securities + '}';
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                Thread.sleep(1000 - (System.currentTimeMillis() % 1000));
                if (isRunning) {
                    nextRound();
                    //System.out.println(this);
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

}
