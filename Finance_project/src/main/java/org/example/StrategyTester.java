package org.example;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

public class StrategyTester {
    private static final double INITIAL_CAPITAL = 1_000_000.0;

    public static double calculateMaxDrawdown(List<BigDecimal> portfolioValues) {
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal peak = portfolioValues.get(0);

        for (BigDecimal value : portfolioValues) {
            peak = peak.max(value);
            BigDecimal currentDrawdown = peak.subtract(value).divide(peak, MathContext.DECIMAL128);
            maxDrawdown = maxDrawdown.max(currentDrawdown);
        }

        return maxDrawdown.doubleValue();
    }

    private static void simulate() {
        System.out.println("Simulation started.");

        StockDataManager dataManager = new StockDataManager();
        dataManager.loadHistoricalDataFromCSV("stock_data/consolidated_stock_data_2.csv");
        System.out.println("Data loaded successfully.");

        List<String> stocks = dataManager.getStocks();
        System.out.printf("Total stocks to process: %d%n", stocks.size());

        Map<String, BigDecimal> capitalPerStock = new HashMap<>();
        Map<String, Long> portfolioPerStock = new HashMap<>();
        List<BigDecimal> dailyPortfolioValues = new ArrayList<>();

        Strategy.Glob glob = new Strategy.Glob();
        glob.capital = BigDecimal.valueOf(INITIAL_CAPITAL);

        for (String stock : stocks) {
            System.out.printf("Processing stock: %s%n", stock);

            capitalPerStock.put(stock, BigDecimal.valueOf(INITIAL_CAPITAL / stocks.size()));
            portfolioPerStock.put(stock, 0L);

            List<StockData> stockData = dataManager.getHistoricalData(stock);
            List<BigDecimal> closingPricesList = new ArrayList<>();
            List<BigDecimal> highs = new ArrayList<>();
            List<BigDecimal> lows = new ArrayList<>();

            for (int i = 14; i < stockData.size(); i++) {  // Start after sufficient data for indicators
                StockData data = stockData.get(i);
                System.out.printf("  Processing date: %s%n", data.getDate());

                // Store price data for indicator calculations
                closingPricesList.add(data.getClose());
                highs.add(data.getHigh());
                lows.add(data.getLow());

                // Compute indicators
                System.out.println("  Calculating indicators...");
                BigDecimal volHawkes2 = Indicators.calculateVolatility(closingPricesList, 14);
                BigDecimal vol2 = Indicators.calculateVolatility(closingPricesList, 7);
                BigDecimal rsi = Indicators.calculateRSI(closingPricesList, 14);
                BigDecimal lsma = Indicators.calculateLSMA(closingPricesList, 14);
                BigDecimal[] gaussianFilter = Indicators.getGaussianFilter(closingPricesList, 14, 2);
                BigDecimal adx = Indicators.calculateADX(highs, lows, closingPricesList, 14);
                BigDecimal zscoreVolatility = Indicators.calculateZScore(closingPricesList, 14);
                BigDecimal temaCurr = Indicators.calculateTEMA(closingPricesList, 14);
                BigDecimal temaPrev = Indicators.calculatePreviousTEMA(closingPricesList, 14);

                System.out.println("  Indicators calculated.");

                BigDecimal closingPrice = data.getAdjClose();

                // Long Entry
                if (Strategy.checkLongEntry(data, glob, closingPricesList, gaussianFilter, lsma)) {
                    System.out.println("  Long entry triggered.");
                    long bought = Math.min(capitalPerStock.get(stock).divide(closingPrice, MathContext.DECIMAL128).longValue(), data.getVolume());
                    portfolioPerStock.put(stock, portfolioPerStock.get(stock) + bought);
                    capitalPerStock.put(stock, capitalPerStock.get(stock).subtract(closingPrice.multiply(BigDecimal.valueOf(bought))));
                    System.out.printf("  Bought %d shares of %s at $%.2f%n", bought, stock, closingPrice.doubleValue());
                }

                // Short Entry
                if (Strategy.checkShortEntry(data, glob, closingPricesList, gaussianFilter, lsma)) {
                    System.out.println("  Short entry triggered.");
                    long sold = Math.min(portfolioPerStock.get(stock), data.getVolume());
                    portfolioPerStock.put(stock, portfolioPerStock.get(stock) - sold);
                    capitalPerStock.put(stock, capitalPerStock.get(stock).add(closingPrice.multiply(BigDecimal.valueOf(sold))));
                    System.out.printf("  Short sold %d shares of %s at $%.2f%n", sold, stock, closingPrice.doubleValue());
                }

                // Long Exit
                if (glob.currPosition == 1 && Strategy.checkLongExit(data, glob)) {
                    System.out.println("  Long exit triggered.");
                    long sold = portfolioPerStock.get(stock);
                    portfolioPerStock.put(stock, 0L);
                    capitalPerStock.put(stock, capitalPerStock.get(stock).add(closingPrice.multiply(BigDecimal.valueOf(sold))));
                    System.out.printf("  Sold %d shares of %s at $%.2f%n", sold, stock, closingPrice.doubleValue());
                }

                // Short Exit
                if (glob.currPosition == -1 && Strategy.checkShortExit(data, glob)) {
                    System.out.println("  Short exit triggered.");
                    long bought = Math.min(capitalPerStock.get(stock).divide(closingPrice, MathContext.DECIMAL128).longValue(), data.getVolume());
                    portfolioPerStock.put(stock, portfolioPerStock.get(stock) + bought);
                    capitalPerStock.put(stock, capitalPerStock.get(stock).subtract(closingPrice.multiply(BigDecimal.valueOf(bought))));
                    System.out.printf("  Covered %d shares of %s at $%.2f%n", bought, stock, closingPrice.doubleValue());
                }

                // Update daily portfolio value
                BigDecimal portfolioValue = capitalPerStock.get(stock).add(closingPrice.multiply(BigDecimal.valueOf(portfolioPerStock.get(stock))));
                if (dailyPortfolioValues.size() <= i - 14) {
                    dailyPortfolioValues.add(portfolioValue);
                } else {
                    dailyPortfolioValues.set(i - 14, dailyPortfolioValues.get(i - 14).add(portfolioValue));
                }

                System.out.printf("  Portfolio value updated: $%.2f%n", portfolioValue.doubleValue());
            }
        }

        // Calculate Sharpe ratio
        List<BigDecimal> dailyReturns = new ArrayList<>();
        for (int i = 1; i < dailyPortfolioValues.size(); i++) {
            BigDecimal dailyReturn = dailyPortfolioValues.get(i).subtract(dailyPortfolioValues.get(i - 1))
                    .divide(dailyPortfolioValues.get(i - 1), MathContext.DECIMAL128);
            dailyReturns.add(dailyReturn);
        }

        BigDecimal averageReturn = dailyReturns.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyReturns.size()), MathContext.DECIMAL128);
        BigDecimal variance = dailyReturns.stream()
                .map(r -> r.subtract(averageReturn).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyReturns.size()), MathContext.DECIMAL128);
        BigDecimal standardDeviation = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
        BigDecimal sharpeRatio = averageReturn.divide(standardDeviation, MathContext.DECIMAL128);

        // Calculate performance metrics
        double maxDrawdown = calculateMaxDrawdown(dailyPortfolioValues);
        BigDecimal finalCapital = dailyPortfolioValues.get(dailyPortfolioValues.size() - 1);

        System.out.println("Backtest Results:");
        System.out.printf("Initial Capital: $%.2f%n", INITIAL_CAPITAL);
        System.out.printf("Final Capital: $%.2f%n", finalCapital.doubleValue());
        System.out.printf("Max Drawdown: %.2f%%%n", maxDrawdown * 100);
        System.out.printf("Annualized Sharpe Ratio: %.6f%n%n", sharpeRatio.doubleValue() * Math.sqrt(252));

        System.out.println("Simulation complete.");
    }

    public static void main(String[] args) {
        simulate();
    }
}
