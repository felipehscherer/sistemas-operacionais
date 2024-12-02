package util;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StatisticsCalculator {

    public static double getMinimum(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }
        return Collections.min(data);
    }

    public static double getMaximum(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }
        return Collections.max(data);
    }

    public static double getMean(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }
        return data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public static double getMedian(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }
        int size = data.size();
        List<Double> sortedData = data.stream().sorted().collect(Collectors.toList());
        if (size % 2 == 0) {
            return (sortedData.get(size / 2 - 1) + sortedData.get(size / 2)) / 2.0;
        } else {
            return sortedData.get(size / 2);
        }
    }

    public static double getStandardDeviation(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }
        double mean = getMean(data);
        double sumSquaredDifferences = data.stream()
                .mapToDouble(val -> (val - mean) * (val - mean))
                .sum();
        return Math.sqrt(sumSquaredDifferences / data.size());
    }
}
