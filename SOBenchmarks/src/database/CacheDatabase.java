package database;

public class CacheDatabase extends Database{
    @Override
    public int read(int position) {
        return 0;
    }

    @Override
    public void write(int position, int value) {

    }

    @Override
    public int sum() {
        return 0;
    }
}