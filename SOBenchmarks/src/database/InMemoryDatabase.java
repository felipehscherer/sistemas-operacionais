package database;

import java.util.concurrent.locks.ReentrantLock;

public class InMemoryDatabase extends Database {
    private final int[] data;
    private final ReentrantLock[] locks;
    private final boolean criticalSectionEnabled;

    public InMemoryDatabase(int size, boolean criticalSectionEnabled) {
        this.criticalSectionEnabled = criticalSectionEnabled;
        data = new int[size];
        locks = new ReentrantLock[size];
        for (int i = 0; i < size; i++) {
            locks[i] = new ReentrantLock();
        }
        initializeData();
    }

    private void initializeData() {
        for (int i = 0; i < data.length; i++) {
            data[i] = 0; // Inicializa com valores zero
        }
    }

    public int read(int position) {
        if (criticalSectionEnabled) {
            locks[position].lock();
        }
        try {
            return data[position];
        } finally {
            if (criticalSectionEnabled) {
                locks[position].unlock();
            }
        }
    }

    public void write(int position, int value) {
        if (criticalSectionEnabled) {
            locks[position].lock();
        }
        try {
            data[position] += value; // Corrige a operação de escrita para adicionar o valor
        } finally {
            if (criticalSectionEnabled) {
                locks[position].unlock();
            }
        }
    }

    public int sum() {
        int sum = 0;
        if (criticalSectionEnabled) {
            for (int i = 0; i < data.length; i++) {
                locks[i].lock();
            }
        }
        try {
            for (int i = 0; i < data.length; i++) {
                sum += data[i];
            }
        } finally {
            if (criticalSectionEnabled) {
                for (int i = 0; i < data.length; i++) {
                    locks[i].unlock();
                }
            }
        }
        return sum;
    }
}