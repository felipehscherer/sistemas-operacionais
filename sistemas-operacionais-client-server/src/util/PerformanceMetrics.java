package util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;

import static util.Log.log;

public class PerformanceMetrics {
    private long initialStartCpuTime;
    private long initialStartMemoryUsage;
    private long initialStartTime;

    private long lastCpuTime;
    private long lastMemoryUsage;
    private long lastTime;

    private List<Double> cpuUsageList = new ArrayList<>();
    private List<Double> memoryUsageList = new ArrayList<>();
    private List<Double> latencyList = new ArrayList<>();

    private static final DecimalFormat df = new DecimalFormat("#.###");

    public void start() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        initialStartCpuTime = threadMXBean.getCurrentThreadCpuTime();
        initialStartMemoryUsage = memoryMXBean.getHeapMemoryUsage().getUsed();
        initialStartTime = System.nanoTime();

        lastCpuTime = initialStartCpuTime;
        lastMemoryUsage = initialStartMemoryUsage;
        lastTime = initialStartTime;
    }

    public void measureAndRecord() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        long currentCpuTime = threadMXBean.getCurrentThreadCpuTime();
        long currentMemoryUsage = memoryMXBean.getHeapMemoryUsage().getUsed();
        long currentTime = System.nanoTime();

        if (currentTime >= lastTime) {
            double elapsedTimeDelta = (currentTime - lastTime) / 1_000_000.0; // em milissegundos

            if (currentCpuTime >= lastCpuTime) {
                double cpuTimeDelta = (currentCpuTime - lastCpuTime) / 1_000_000.0; // em milissegundos
                double cpuUsage = (cpuTimeDelta / elapsedTimeDelta) * 100.0; // percentual de uso da CPU
                cpuUsageList.add(cpuUsage);
                lastCpuTime = currentCpuTime;
            } else {
                // Tratar caso de tempo de CPU negativo
                System.err.println("Aviso: Tempo de CPU diminuiu. Possível inconsistência nas medições.");
            }

            double memoryUsage = (currentMemoryUsage - lastMemoryUsage) / (1024.0 * 1024.0);
            memoryUsageList.add(memoryUsage);
            lastMemoryUsage = currentMemoryUsage;

            double latency = elapsedTimeDelta;
            latencyList.add(latency);
            lastTime = currentTime;
        } else {
            log("Aviso: Tempo do sistema diminuiu. Possível inconsistência nas medições.");
        }
    }

    public void printMetrics() {
        System.out.println("Métricas de Desempenho:");

        long endTime = System.nanoTime();
        double totalExecutionTime = (endTime - initialStartTime) / 1_000_000_000.0; // em segundos
        System.out.println("Tempo total de execução: " + formatDouble(totalExecutionTime) + " segundos\n");

        System.out.println("Uso de CPU:");
        printStatistics(cpuUsageList, "% (percentual)", "Percentual de uso de CPU");

        System.out.println("\nUso de Memória:");
        printStatistics(memoryUsageList, "MB (Megabytes)", "Variação de memória heap");

        System.out.println("\nLatência:");
        printStatistics(latencyList, "ms (milissegundos)", "Tempo entre medições");
    }

    private void printStatistics(List<Double> data, String unit, String description) {
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
