package org.example;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

public class Indicators {

    // Calculate Volatility (Standard deviation of returns)
    public static BigDecimal calculateVolatility(List<BigDecimal> closingPrices, int period) {
        List<BigDecimal> returns = calculateReturns(closingPrices);
        BigDecimal mean = BigDecimal.ZERO;
        BigDecimal variance = BigDecimal.ZERO;

        if (returns.size() > 0) {
            for (BigDecimal ret : returns) {
                mean = mean.add(ret);
            }

            mean = mean.divide(BigDecimal.valueOf(returns.size()), MathContext.DECIMAL128);
        } else {
            // Handle the case where returns list is empty (e.g., set mean to 0, or return an error)
            mean = BigDecimal.ZERO;  // You can handle this as per your requirements
        }


        // Calculate variance
        if(returns.size() > 0){
            for (BigDecimal ret : returns) {
                variance = variance.add(ret.subtract(mean).pow(2));
            }
            variance = variance.divide(BigDecimal.valueOf(returns.size()), MathContext.DECIMAL128);

            // Return standard deviation (volatility)
            return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
        }
        else {
            // Return zero if there are no returns (i.e., empty list)
            return BigDecimal.ZERO;
        }


    }

    // Calculate RSI
    public static BigDecimal calculateRSI(List<BigDecimal> prices, int period) {
        // If not enough data to calculate RSI, return BigDecimal.ZERO or any default value
        if (prices.size() < period) {
            return BigDecimal.ZERO;
        }

        BigDecimal gain = BigDecimal.ZERO;
        BigDecimal loss = BigDecimal.ZERO;

        // Start looping from the `period`-th index (i = period) to calculate the initial average gains and losses
        for (int i = 1; i < period; i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gain = gain.add(change);
            } else {
                loss = loss.add(change.negate());
            }
        }

        // Calculate the average gain and loss for the first `period` days
        BigDecimal avgGain = gain.divide(BigDecimal.valueOf(period), MathContext.DECIMAL128);
        BigDecimal avgLoss = loss.divide(BigDecimal.valueOf(period), MathContext.DECIMAL128);

        // If there is no loss, return 100 (extremely bullish), or if there is no gain, return 0 (extremely bearish)
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        if (avgGain.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Calculate Relative Strength (RS)
        BigDecimal rs = avgGain.divide(avgLoss, MathContext.DECIMAL128);

        // Calculate RSI (Relative Strength Index)
        return BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100).divide(rs.add(BigDecimal.ONE), MathContext.DECIMAL128));
    }



    // Calculate LSMA
    public static BigDecimal calculateLSMA(List<BigDecimal> prices, int period) {
        // Handle the case where there isn't enough data to calculate LSMA
        if (prices.size() < period) {
            return BigDecimal.ZERO; // Or another default value of your choice
        }

        BigDecimal x = BigDecimal.ZERO;
        BigDecimal y = BigDecimal.ZERO;
        BigDecimal numerator = BigDecimal.ZERO;
        BigDecimal denominator = BigDecimal.ZERO;

        // Compute the necessary values for LSMA
        for (int i = 0; i < period; i++) {
            BigDecimal xi = BigDecimal.valueOf(i);
            x = x.add(xi);
            y = y.add(prices.get(i));

            numerator = numerator.add(xi.multiply(prices.get(i)));
            denominator = denominator.add(xi.multiply(xi));
        }

        // Avoid division by zero in the case where all xi values are the same
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO; // Return a default value if there's no variation in the x values
        }

        // Calculate the slope and intercept
        BigDecimal slope = numerator.subtract(x.multiply(y).divide(BigDecimal.valueOf(period), MathContext.DECIMAL128))
                .divide(denominator.subtract(x.multiply(x).divide(BigDecimal.valueOf(period), MathContext.DECIMAL128)),
                        MathContext.DECIMAL128);

        BigDecimal intercept = y.subtract(slope.multiply(x));

        // Return the LSMA value at the last index of the period
        return slope.multiply(BigDecimal.valueOf(period - 1)).add(intercept);
    }


    // Calculate the Z-Score for volatility
    public static BigDecimal calculateZScore(List<BigDecimal> returns, int period) {
        BigDecimal mean = BigDecimal.ZERO;
        BigDecimal stdDev = BigDecimal.ZERO;

        for (BigDecimal ret : returns) {
            mean = mean.add(ret);
        }

        mean = mean.divide(BigDecimal.valueOf(period), MathContext.DECIMAL128);

        for (BigDecimal ret : returns) {
            stdDev = stdDev.add(ret.subtract(mean).pow(2));
        }

        stdDev = BigDecimal.valueOf(Math.sqrt(stdDev.doubleValue() / period));

        return mean.divide(stdDev, MathContext.DECIMAL128);
    }

    // Calculate the previous TEMA for the last 15 data points (not including today)
    public static BigDecimal calculatePreviousTEMA(List<BigDecimal> prices, int period) {
        // Handle case where there are not enough prices for TEMA calculation
        if (prices == null || prices.size() < period + 1) {
            return BigDecimal.ZERO;  // Or another default value
        }

        // Get the last 'period' prices excluding the most recent one (not counting today)
        List<BigDecimal> lastPrices = prices.subList(prices.size() - period - 1, prices.size() - 1);

        return calculateTEMA(lastPrices, period);
    }



    // Calculate the TEMA (Triple Exponential Moving Average)
    public static BigDecimal calculateTEMA(List<BigDecimal> prices, int period) {
        // Handle the case where there is not enough data
        if (prices == null || prices.size() < period) {
            return BigDecimal.ZERO;  // Or another default value
        }

        // Calculate the three EMAs needed for TEMA
        BigDecimal ema1 = calculateEMA(prices, period);
        List<BigDecimal> sublist = prices.subList(0, period);

        // Handle edge case where sublist might not be large enough
        if (sublist.size() < period) {
            return BigDecimal.ZERO;
        }

        BigDecimal ema2 = calculateEMA(sublist, period);
        BigDecimal ema3 = calculateEMA(sublist, period);

        // Calculate TEMA using the formula
        return ema1.multiply(BigDecimal.valueOf(3))
                .subtract(ema2.multiply(BigDecimal.valueOf(3)))
                .add(ema3);
    }

    // Calculate EMA (Exponential Moving Average)
    private static BigDecimal calculateEMA(List<BigDecimal> prices, int period) {
        // Handle the case where there is not enough data for the EMA
        if (prices == null || prices.size() < period) {
            return BigDecimal.ZERO;  // Or another default value
        }

        BigDecimal multiplier = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(period + 1), MathContext.DECIMAL128);
        BigDecimal ema = prices.get(0);  // The first value in the list is the initial EMA value

        for (int i = 1; i < prices.size(); i++) {
            ema = prices.get(i).multiply(multiplier).add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
        }
        return ema;
    }


    // Calculate returns
    private static List<BigDecimal> calculateReturns(List<BigDecimal> prices) {
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            BigDecimal returnValue = prices.get(i).subtract(prices.get(i - 1))
                    .divide(prices.get(i - 1), MathContext.DECIMAL128);
            returns.add(returnValue);
        }
        return returns;
    }

    // Gaussian Filter Calculation
    public static BigDecimal[] getGaussianFilter(List<BigDecimal> data, int cyclePeriod, int poles) {
        final double PI = Math.PI;
        double beta = (1 - Math.cos(2 * PI / cyclePeriod)) / (Math.pow(2, 1.0 / poles) - 1);
        double alpha = -beta + Math.sqrt(Math.pow(beta, 2) + 2 * beta);

        BigDecimal[] filterArr = new BigDecimal[data.size()];
        filterArr[0] = data.get(0);

        for (int i = 1; i < data.size(); i++) {
            if (poles == 1) {
                filterArr[i] = BigDecimal.valueOf(alpha * data.get(i).doubleValue() + (1 - alpha) * filterArr[i - 1].doubleValue());
            } else if (poles == 2) {
                if (i >= 2) {
                    filterArr[i] = BigDecimal.valueOf(Math.pow(alpha, 2) * data.get(i).doubleValue()
                            + 2 * (1 - alpha) * filterArr[i - 1].doubleValue()
                            - Math.pow(1 - alpha, 2) * filterArr[i - 2].doubleValue());
                } else {
                    filterArr[i] = data.get(i);
                }
            }
        }
        return filterArr;
    }

    // Calculate the Average Directional Index (ADX)
    public static BigDecimal calculateADX(List<BigDecimal> highs, List<BigDecimal> lows, List<BigDecimal> closes, int period) {
        // Handle the case where the data is insufficient for the given period
        if (highs.size() < period || lows.size() < period || closes.size() < period) {
            return BigDecimal.ZERO; // Or any other suitable default value
        }

        BigDecimal plusDM = BigDecimal.ZERO;
        BigDecimal minusDM = BigDecimal.ZERO;
        BigDecimal tr = BigDecimal.ZERO;

        // Calculate the True Range (TR) and Directional Movement (DM)
        for (int i = 1; i < period; i++) {
            BigDecimal highChange = highs.get(i).subtract(highs.get(i - 1));
            BigDecimal lowChange = lows.get(i).subtract(lows.get(i - 1));

            plusDM = plusDM.add(highChange.compareTo(BigDecimal.ZERO) > 0 ? highChange : BigDecimal.ZERO);
            minusDM = minusDM.add(lowChange.compareTo(BigDecimal.ZERO) > 0 ? lowChange : BigDecimal.ZERO);
            tr = tr.add(highs.get(i).subtract(lows.get(i)));
        }

        // Handle case where TR is zero to avoid division by zero
        if (tr.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO; // Or return a default value, like zero, if no true range
        }

        // Calculate the averages for +DM, -DM, and TR
        BigDecimal avgPlusDM = plusDM.divide(BigDecimal.valueOf(period), MathContext.DECIMAL128);
        BigDecimal avgMinusDM = minusDM.divide(BigDecimal.valueOf(period), MathContext.DECIMAL128);
        BigDecimal avgTR = tr.divide(BigDecimal.valueOf(period), MathContext.DECIMAL128);

        // Calculate the +DI and -DI
        BigDecimal plusDI = avgPlusDM.divide(avgTR, MathContext.DECIMAL128).multiply(BigDecimal.valueOf(100));
        BigDecimal minusDI = avgMinusDM.divide(avgTR, MathContext.DECIMAL128).multiply(BigDecimal.valueOf(100));

        // Calculate the ADX (average of the absolute difference between +DI and -DI)
        BigDecimal adx = plusDI.subtract(minusDI).abs();
        return adx;
    }


}
