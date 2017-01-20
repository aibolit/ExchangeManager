/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package exchangemanager;

import ExchangeObjects.ProbabilityDistribution;
import ExchangeObjects.ProbabilityDistributions.CyclicValueDistribution;
import ExchangeObjects.ProbabilityDistributions.RandomDistribution;
import ExchangeObjects.ProbabilityDistributions.RandomWalkDistribution;
import ExchangeObjects.ProbabilityDistributions.SingleValueDistribution;
import ExchangeObjects.Security;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 *
 * @author Sasa
 */
public class Configurations {

    public final static String EXCHANGE_USER = "EXCHANGE";
    private static int port = 14739;
    private static String host = "127.0.0.1";
    private static final Map<String, String> users = new ConcurrentHashMap<>();
    private static double initialCash = 1000000;
    private static final Set<Security> securities = new ConcurrentSkipListSet<>(new Comparator<Security>() {
        @Override
        public int compare(Security t0, Security t1) {
            return t0.getTicker().compareTo(t1.getTicker());
        }
    });
    private static final Map<String, String> securitySigniatures = new ConcurrentHashMap<>();
    private static double exchangePriceMultiplier = 200;
    private static double visibleShares = .4;
    private static double priceValuationAlpha = .4;
    private static double priceValuationTargetShares = .2;
    private static long timeout = 50;
    private static int maxConnectionsPerUser = 3;
    private static double dividendGrowthFactor = .996288;
    private static double exchangeBuyFactor = 2, exchangeSellFactor = .5;
    private static double exchangeValuationAlpha = .03;
    private static Long ticksRemaining = null;
    private static Long downtimeTicks = null;
    private static double dividendRegenFactor = .002;

    private static void init() {
        users.clear();
        users.put(EXCHANGE_USER, "*applecherrytaco*");
        securities.clear();
        //DEMO DATA
    }

    public static int getPort() {
        return port;
    }

    public static String getHost() {
        return host;
    }

    public static double getInitialCash() {
        return initialCash;
    }

    public static double getVisibleShares() {
        return visibleShares;
    }

    public static Set<String> getUsers() {
        return users.keySet();
    }

    public static String getUserPassword(String user) {
        if (!users.containsKey(user)) {
            throw new IllegalArgumentException("No Such User");
        }
        return users.get(user);
    }

    public static Set<Security> getSecurities() {
        return Collections.unmodifiableSet(securities);
    }

    public static double getExchangePriceMultiplier() {
        return exchangePriceMultiplier;
    }

    public static double getPriceValuationAlpha() {
        return priceValuationAlpha;
    }

    public static double getPriceValuationTargetShares() {
        return priceValuationTargetShares;
    }

    public static long getTimeout() {
        return timeout;
    }

    public static int getMaxConnectionsPerUser() {
        return maxConnectionsPerUser;
    }

    public static double getDividendGrowthFactor() {
        return dividendGrowthFactor;
    }

    public static double getExchangeBuyFactor() {
        return exchangeBuyFactor;
    }

    public static double getExchangeSellFactor() {
        return exchangeSellFactor;
    }

    public static double getExchangeValuationAlpha() {
        return exchangeValuationAlpha;
    }

    public static Long getTicksRemaining() {
        return ticksRemaining;
    }

    public static Long getDowntimeTicks() {
        return downtimeTicks;
    }

    public static double getDividendRegenFactor() {
        return dividendRegenFactor;
    }

    public static void readCongfigs(String file) throws IOException {
        init();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0) continue;
                //System.out.println(line);
                StringTokenizer st = new StringTokenizer(line);
                switch (st.nextToken()) {
                    case "port":
                        port = Integer.parseInt(st.nextToken());
                        break;
                    case "host":
                        host = st.nextToken();
                        break;
                    case "users":
                        while (st.hasMoreTokens()) {
                            users.put(st.nextToken(), st.nextToken());
                        }
                        break;
                    case "initial-cash":
                        initialCash = Double.parseDouble(st.nextToken());
                        break;
                    case "exchange-price-multiplier":
                        exchangePriceMultiplier = Double.parseDouble(st.nextToken());
                        break;
                    case "visible-shares":
                        visibleShares = Double.parseDouble(st.nextToken());
                        break;
                    case "security":
                        Security security = new Security(st.nextToken(), Integer.parseInt(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), parseDistribution(st));
                        securities.add(security);
                        securitySigniatures.put(security.getTicker(), line);
                        break;
                    case "price-valuation":
                        priceValuationAlpha = Double.parseDouble(st.nextToken());
                        priceValuationTargetShares = Double.parseDouble(st.nextToken());
                        break;
                    case "timeout":
                        timeout = Long.parseLong(st.nextToken());
                        break;
                    case "max-connections":
                        maxConnectionsPerUser = Integer.parseInt(st.nextToken());
                        break;
                    case "dividend-growth-factor":
                        dividendGrowthFactor = Double.parseDouble(st.nextToken());
                        break;
                    case "exchange-trade-factors":
                        exchangeBuyFactor = Double.parseDouble(st.nextToken());
                        exchangeSellFactor = Double.parseDouble(st.nextToken());
                        break;
                    case "exchange-valuation-alpha":
                        exchangeValuationAlpha = Double.parseDouble(st.nextToken());
                        break;
                    case "ticks-remaining":
                        ticksRemaining = Long.parseLong(st.nextToken());
                        break;
                    case "downtime-ticks":
                        downtimeTicks = Long.parseLong(st.nextToken());
                        break;
                    case "dividend-regen-factor":
                        dividendRegenFactor = Double.parseDouble(st.nextToken());
                        break;
                    default:
                        if (line.charAt(0) != '#') {
                            System.out.println("Oops no such setting " + line);
                        }
                        break;
                }
            }
        }
    }

    public static void saveConfigurations(String file) throws IOException {
        try (PrintWriter pw = new PrintWriter(new File(file))) {
            pw.print(getConfigString());
        }
    }

    private static ProbabilityDistribution parseDistribution(StringTokenizer st) {
        String type = st.nextToken();
        switch (type) {
            case "cyclic":
                List<Double> data = new ArrayList<>();
                do {
                    data.add(Double.parseDouble(st.nextToken()));
                } while (st.hasMoreTokens());
                return new CyclicValueDistribution(data);
            case "random":
                if (!st.hasMoreTokens()) {
                    return new RandomDistribution();
                } else {
                    return new RandomDistribution(Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()));
                }
            case "walk":
                if (!st.hasMoreTokens()) {
                    return new RandomWalkDistribution();
                } else {
                    return new RandomWalkDistribution(Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()));
                }
            case "single":
                return new SingleValueDistribution(Double.parseDouble(st.nextToken()));
            default:
                throw new AssertionError();
        }
    }

    public static String getConfigString() {
        StringBuilder out = new StringBuilder();
        out.append("port ").append(port).append("\nhost ").append(host).append("\n");
        out.append("users ");
        for (Map.Entry<String, String> user : users.entrySet()) {
            out.append(user.getKey()).append(" ").append(user.getValue());
        }
        out.append("\n");
        out.append("initial-cash ").append(initialCash).append("\n");
        out.append("exchange-price-multiplier ").append(exchangePriceMultiplier).append("\n");
        out.append("visible-shares ").append(visibleShares).append("\n");
        out.append("price-valuation ").append(priceValuationAlpha).append(" ").append(priceValuationTargetShares).append("\n");
        out.append("timeout ").append(timeout).append("\n");
        out.append("dividend-growth-factor ").append(dividendGrowthFactor).append("\n");
        out.append("exchange-trade-factors ").append(exchangeBuyFactor).append(" ").append(exchangeSellFactor).append("\n");
        out.append("exchange-valuation-alpha ").append(exchangeValuationAlpha).append("\n");
        out.append("ticks-remaining ").append(ticksRemaining).append("\n");
        for (Security security : securities) {
            out.append(securitySigniatures.get(security.getTicker())).append("\n");
        }
        return out.toString();
    }

    private Configurations() {

    }
}
