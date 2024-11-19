package database;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;

public class InMemoryDatabase extends Database {
    private final int[] data;
    private final ReadWriteLock lock;
    private final boolean criticalSectionEnabled;

    public InMemoryDatabase(int size, boolean criticalSectionEnabled) {
        this.criticalSectionEnabled = criticalSectionEnabled;
        data = new int[size];
        lock = new ReentrantReadWriteLock();
        initializeData();
    }

    private void initializeData() {
        for (int i = 0; i < data.length; i++) {
            data[i] = 0; // Inicializa com valores zero
        }
    }

    @Override
    public int read(int position) {
        if (criticalSectionEnabled) {
            lock.readLock().lock();
        }
        try {
            return data[position];
        } finally {
            if (criticalSectionEnabled) {
                lock.readLock().unlock();
            }
        }
    }

    @Override
    public void write(int position, int value) {
        if (criticalSectionEnabled) {
            lock.writeLock().lock();
        }
        try {
            data[position] += value; // Adiciona o valor à posição
        } finally {
            if (criticalSectionEnabled) {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public int sum() {
        if (criticalSectionEnabled) {
            lock.readLock().lock();
        }
        try {
            int sum = 0;
            for (int value : data) {
                sum += value;
            }
            return sum;
        } finally {
            if (criticalSectionEnabled) {
                lock.readLock().unlock();
            }
        }
    }

    public int getSize() {
        return data.length;
    }
}