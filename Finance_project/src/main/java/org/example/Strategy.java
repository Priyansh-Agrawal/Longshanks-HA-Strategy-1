package org.example;

import java.math.BigDecimal;
import java.util.List;

public class Strategy {

    public static class Glob {
        public BigDecimal entryPrice, trailingPrice, capital;
        public int currPosition; // 1 for long, -1 for short, 0 for no position
    }

    // Long entry conditions (LSMA crosses above Gaussian Filter with relaxed thresholds)
    public static boolean checkLongEntry(StockData data, Glob glob, List<BigDecimal> closingPricesList, BigDecimal[] gaussianFilter, BigDecimal lsma) {
        if (glob.currPosition != 0) return false; // No long entry if already in a position

        // Get the latest value of the Gaussian filter
        BigDecimal latestGaussianFilter = gaussianFilter.length > 0 ? gaussianFilter[gaussianFilter.length - 1] : BigDecimal.ZERO;

        // Relaxed condition: LSMA crosses above Gaussian Filter (with relaxed conditions)
        if (lsma.compareTo(latestGaussianFilter) > 0) {
            glob.entryPrice = data.getClose();
            glob.trailingPrice = glob.entryPrice;
            glob.currPosition = 1; // Enter long position
            return true;
        }
        return false;
    }

    // Short entry conditions (Gaussian Filter crosses above LSMA with relaxed thresholds)
    public static boolean checkShortEntry(StockData data, Glob glob, List<BigDecimal> closingPricesList, BigDecimal[] gaussianFilter, BigDecimal lsma) {
        if (glob.currPosition != 0) return false; // No short entry if already in a position

        // Get the latest value of the Gaussian filter
        BigDecimal latestGaussianFilter = gaussianFilter.length > 0 ? gaussianFilter[gaussianFilter.length - 1] : BigDecimal.ZERO;

        // Relaxed condition: Gaussian Filter crosses above LSMA (with relaxed conditions)
        if (latestGaussianFilter.compareTo(lsma) > 0) {
            glob.entryPrice = data.getClose();
            glob.trailingPrice = glob.entryPrice;
            glob.currPosition = -1; // Enter short position
            return true;
        }
        return false;
    }

    // Long exit conditions (price falls below entry price with more lenient thresholds)
    public static boolean checkLongExit(StockData data, Glob glob) {
        if (glob.currPosition != 1) return false; // No exit if not in a long position

        BigDecimal close = data.getClose();
        // Relax exit conditions (e.g., 3% exit threshold instead of 5%)
        boolean priceCondition = close.compareTo(glob.entryPrice.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(0.03)))) >= 0 ||  // Exit when price rises 3%
                close.compareTo(glob.entryPrice.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(0.03)))) <= 0;  // Exit when price drops 3%

        return priceCondition;
    }

    // Short exit conditions (price rises above entry price with more lenient thresholds)
    public static boolean checkShortExit(StockData data, Glob glob) {
        if (glob.currPosition != -1) return false; // No exit if not in a short position

        BigDecimal close = data.getClose();
        // Relax exit conditions (e.g., 3% exit threshold instead of 5%)
        return close.compareTo(glob.entryPrice.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(0.03)))) <= 0 ||  // Exit when price drops 3%
                close.compareTo(glob.entryPrice.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(0.03)))) >= 0;  // Exit when price rises 3%
    }
}
