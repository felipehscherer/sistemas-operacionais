package database;

public abstract class Database {
    public abstract int read(int position);
    public abstract void write(int position, int value);
    public abstract int sum();
}
