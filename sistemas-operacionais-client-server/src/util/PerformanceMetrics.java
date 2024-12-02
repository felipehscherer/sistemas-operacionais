package util;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.sun.management.OperatingSystemMXBean;

public class PerformanceMetrics {
    private OperatingSystemMXBean osBean;

    private long lastCpuTime;
    private long lastTime;

    private List<Double> cpuUsageList = new ArrayList<>();
    private List<Double> memoryUsageList = new ArrayList<>();
    private List<Double> latencyList = new ArrayList<>();

    private static final DecimalFormat df = new DecimalFormat("#.###");

    public void start() {
        osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        lastCpuTime = osBean.getProcessCpuTime();
        lastTime = System.nanoTime();
    }

    public synchronized void measureAndRecord() {
        double processCpuLoad = osBean.getProcessCpuLoad();
        if (processCpuLoad >= 0) {
            double cpuUsagePercent = processCpuLoad * 100.0;
            cpuUsageList.add(cpuUsagePercent);
        } else {
            System.err.println("Não foi possível obter o uso de CPU do processo.");
        }

        // Memória usada pela JVM em MB
        double memoryUsageMB = (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                / (1024 * 1024);
        memoryUsageList.add(memoryUsageMB);

        // Latência (tempo entre medições) em milissegundos
        long currentTime = System.nanoTime();
        long deltaTime = currentTime - lastTime;
        double latency = deltaTime / 1_000_000.0;
        latencyList.add(latency);
        lastTime = currentTime;
    }


    public void printMetrics() {
        System.out.println("Métricas de Desempenho:");

        System.out.println("\nUso de CPU:");
        printStatistics(cpuUsageList, "% (percentual)", "Percentual de uso de CPU");

        System.out.println("\nUso de Memória:");
        printStatistics(memoryUsageList, "MB (Megabytes)", "Uso de memória heap");

        System.out.println("\nLatência:");
        printStatistics(latencyList, "ms (milissegundos)", "Tempo entre medições");
    }

    private void printStatistics(List<Double> data, String unit, String description) {
        if (data.isEmpty()) {
            System.out.println("  " + description + ": Nenhum dado coletado.");
            return;
        }
        System.out.println("  " + description + ":");
        System.out.println("    Mínimo: " + formatDouble(StatisticsCalculator.getMinimum(data)) + " " + unit);
        System.out.println("    Máximo: " + formatDouble(StatisticsCalculator.getMaximum(data)) + " " + unit);
        System.out.println("    Média: " + formatDouble(StatisticsCalculator.getMean(data)) + " " + unit);
        System.out.println("    Mediana: " + formatDouble(StatisticsCalculator.getMedian(data)) + " " + unit);
        System.out.println("    Desvio Padrão: " + formatDouble(StatisticsCalculator.getStandardDeviation(data)) + " " + unit);
    }

    private String formatDouble(double value) {
        return df.format(value);
    }

    public List<Double> getCpuUsageList() {
        return cpuUsageList;
    }

    public List<Double> getMemoryUsageList() {
        return memoryUsageList;
    }

    public List<Double> getLatencyList() {
        return latencyList;
    }
}
