/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import ExchangeObjects.Exchange;
import ExchangeObjects.ExchangeException;
import ExchangeObjects.Order;
import ExchangeObjects.OrderType;
import ExchangeObjects.Security;
import exchangemanager.Configurations;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Sasa
 */
public class ExchangeServer implements Runnable {

    private volatile boolean isRunning = false;
    private final Exchange exchange;
    private final Map<String, Map<Integer, Socket>> userConnections = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, PrintWriter>> subscribedConnections = new ConcurrentHashMap<>();

    public ExchangeServer(Exchange exchange) {
        this.exchange = exchange;
        for (String user : Configurations.getUsers()) {
            userConnections.put(user, new ConcurrentHashMap<Integer, Socket>());
            subscribedConnections.put(user, new ConcurrentHashMap<Integer, PrintWriter>());
        }
    }

    public Exchange getExchange() {
        return exchange;
    }

    private Integer newConnection(String user, Socket socket) {
        synchronized (userConnections) {
            Map<Integer, Socket> conns = userConnections.get(user);
            for (int i = 0; i < Configurations.getMaxConnectionsPerUser(); i++) {
                if (!conns.containsKey(i)) {
                    conns.put(i, socket);
                    return i;
                }
            }
        }
        return null;
    }

    private void removeConnection(String user, int id) {
        synchronized (subscribedConnections) {
            subscribedConnections.get(user).remove(id);
        }
        synchronized (userConnections) {
            try {
                Socket socket = userConnections.get(user).get(id);
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {

            }
            userConnections.get(user).remove(id);
        }
    }

    private void writeToSubscriptions(Map<String, List<String>> subData) {
        synchronized (subscribedConnections) {
            for (Map.Entry<String, List<String>> entry : subData.entrySet()) {
                String subUser = entry.getKey();
                List<String> subActions = entry.getValue();

                for (Map.Entry<Integer, PrintWriter> subEntry : subscribedConnections.get(subUser).entrySet()) {
                    Integer subKey = subEntry.getKey();
                    PrintWriter subPout = subEntry.getValue();
                    try {
                        for (String subAction : subActions) {
                            subPout.println(subAction);
                        }
                        subPout.flush();
                    } catch (Exception ex) {
                        removeConnection(subUser, subKey);
                    }
                }
            }
        }
    }

    public boolean isIsRunning() {
        return isRunning;
    }

    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
        if(exchange != null) exchange.setRun(isRunning);
    }

    @Override
    public void run() {
        isRunning = true;
        try {
            new Thread() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            synchronized (exchange) {
                                exchange.wait(1000);
                            }
                        } catch (InterruptedException ex) {
                        } finally {
                            synchronized (exchange) {
                                if (!exchange.getUserUpdates().isEmpty()) {
                                    writeToSubscriptions(exchange.getUserUpdates());
                                    exchange.clearUserUpdates();
                                }
                            }
                        }

                    }
                }
            }.start();

            ServerSocket serverSocket = new ServerSocket(Configurations.getPort());
            while (!serverSocket.isClosed()) {
                final Socket socket = serverSocket.accept();
                new Thread() {
                    @Override
                    public void run() {
                        String user = null;
                        Integer connectionId = null;
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            PrintWriter out = new PrintWriter(socket.getOutputStream())) {
                            StringTokenizer st = new StringTokenizer(in.readLine());
                            if (!st.hasMoreTokens()) {
                                out.println("Unknown User");
                                return;
                            }
                            user = st.nextToken();
                            if (!st.hasMoreTokens()) {
                                out.println("Unknown User");
                                return;
                            }
                            String password = st.nextToken();
                            if (!Configurations.getUsers().contains(user) || !Configurations.getUserPassword(user).equals(password)) {
                                out.println("Unknown User");
                                return;
                            }
                            this.setName("Connection-" + user);

                            if ((connectionId = newConnection(user, socket)) == null) {
                                out.println("You have already reached the maximum connection limit");
                                return;
                            }

                            String line;
                            while ((line = in.readLine()) != null && !line.trim().equals("CLOSE_CONNECTION")) {
                                try {
                                    String val;
                                    if (isRunning) {
                                        val = processCommand(user, line, connectionId, out);
                                    } else {
                                        val = "SERVER_NOT_ACTIVE";
                                    }
                                    synchronized (subscribedConnections) {
                                        if (val != null) {
                                            out.println(val);
                                        }
                                        out.flush();
                                    }
                                    Thread.sleep(Configurations.getTimeout());
                                } catch (ExchangeException ex) {
                                    synchronized (subscribedConnections) {
                                        out.println("ERROR " + ex.getMessage());
                                        out.flush();
                                    }
                                } catch (InterruptedException ex) {
                                }
                            }

                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        } finally {
                            if (user != null && connectionId != null) {
                                removeConnection(user, connectionId);
                            }
                        }
                    }

                }.start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private String processCommand(String user, String line, int connectionId, PrintWriter pout) throws ExchangeException {
        StringTokenizer st = new StringTokenizer(line);
        String cmd = st.nextToken();
        String out = null;
        switch (cmd) {
            case "MY_CASH": {
                out = "MY_CASH_OUT " + exchange.getUserCash(user);
            }
            break;
            case "MY_SECURITIES": {
                synchronized (exchange) {
                    out = "MY_SECURITIES_OUT ";
                    Map<Security, Double> dividendFactors = exchange.getUserDividendFactors(user);
                    for (Security security : Configurations.getSecurities()) {
                        int shares = exchange.getUserShares(user).get(security);
                        out += security.getTicker() + " " + shares + " " + dividendFactors.get(security) * shares * security.getDividend() + " ";
                    }
                }
            }
            break;
            case "SECURITIES": {
                out = "SECURITIES_OUT ";
                for (Security security : Configurations.getSecurities()) {
                    out += security.getTicker() + " " + security.getNetWorth() + " " + security.getDividend() + " " + security.getVolatility() + " ";
                }
            }
            break;
            case "ORDERS": {
                if (!st.hasMoreTokens()) {
                    throw new ExchangeException("No Security Specified");
                }
                out = "SECURITY_ORDERS_OUT ";
                synchronized (exchange) {
                    for (Map.Entry<OrderType, SortedSet<Order>> entry : exchange.getOrderStatus(st.nextToken()).entrySet()) {
                        for (Order order : entry.getValue()) {
                            out += order.getType() + " " + order.getSecurity().getTicker() + " " + order.getPrice() + " " + order.getShares() + " ";
                        }
                    }
                }
            }
            break;
            case "MY_ORDERS": {
                out = "MY_ORDERS_OUT ";
                synchronized (exchange) {
                    for (Order order : exchange.getUserOrders(user)) {
                        out += order.getType() + " " + order.getSecurity().getTicker() + " " + order.getPrice() + " " + order.getShares() + " ";
                    }
                }
            }
            break;
            case "CLEAR_BID": {
                if (!st.hasMoreTokens()) {
                    throw new ExchangeException("No Security Specified");
                }
                exchange.removeOrder(user, st.nextToken(), OrderType.BID);
                out = "CLEAR_BID_OUT DONE";
            }
            break;
            case "CLEAR_ASK": {
                if (!st.hasMoreTokens()) {
                    throw new ExchangeException("No Security Specified");
                }
                exchange.removeOrder(user, st.nextToken(), OrderType.ASK);
                out = "CLEAR_ASK_OUT DONE";
            }
            break;
            case "SUBSCRIBE": {
                synchronized (subscribedConnections) {
                    subscribedConnections.get(user).put(connectionId, pout);
                }
            }
            break;
            case "UNSUBSCRIBE": {
                synchronized (subscribedConnections) {
                    subscribedConnections.get(user).remove(connectionId);
                }
            }
            break;
            case "BID": {
                String ticker;
                int shares;
                double price;
                if (!st.hasMoreTokens()) {
                    throw new ExchangeException("No Security Specified");
                }
                ticker = st.nextToken();
                if (!st.hasMoreTokens()) {
                    throw new ExchangeException("No Price Specified");
                }
                try {
                    price = Double.parseDouble(st.nextToken());
                } catch (NumberFormatException ex) {
                    throw new ExchangeException("Price must be a positive number");
                }
                if (!st.hasMoreTokens()) {
                    throw new ExchangeException("No Shares Specified");
                }
                try {
                    shares = Integer.parseInt(st.nextToken());
                } catch (NumberFormatException ex) {
                    throw new ExchangeException("Shares must be a positive integer");
                }
                Map<String, List<String>> subData = exchange.newOrder(exchange.createOrder(user, OrderType.BID, ticker, shares, price));
                writeToSubscriptions(subData);
                out = "BID_OUT DONE";
            }
            break;
            case "ASK": {
                String ticker;
                int shares;
                double price;
                if (!st.hasMoreTokens()) {
                    throw new ExchangeException("No Security Specified");
                }
                ticker = st.nextToken();
                if (!st.hasMoreTokens()) {
                    throw new ExchangeException("No Price Specified");
                }
                try {
                    price = Double.parseDouble(st.nextToken());
                } catch (NumberFormatException ex) {
                    throw new ExchangeException("Price must be a positive number");
                }
                if (!st.hasMoreTokens()) {
                    throw new ExchangeException("No Shares Specified");
                }
                try {
                    shares = Integer.parseInt(st.nextToken());
                } catch (NumberFormatException ex) {
                    throw new ExchangeException("Shares must be a positive integer");
                }
                Map<String, List<String>> subData = exchange.newOrder(exchange.createOrder(user, OrderType.ASK, ticker, shares, price));
                writeToSubscriptions(subData);
                out = "ASK_OUT DONE";
            }
            break;
            default: {
                if (cmd.charAt(0) != '#') {
                    out = "Unknown Command " + line;
                }
            }
            break;
        }
        return out;
    }
}
