package datastructure;

import java.util.*;

/**
 * Created by fx on 12/10/2016.
 */
public class HMDataStructure {
    public Map<String, Object> ds;

    public HMDataStructure() {
        ds = new HashMap<String, Object>();
    }

    public void add(String key, double value) {
        ds.put(key, value);
    }
    public void add(String key, int value) {
        ds.put(key, value);
    }

    public void add(String key, String value) {
        ds.put(key, value);
    }

    public int getTimestamp(String timestamp) {
        return (int)ds.get(timestamp);
    }

    public int getInt(String key) {
        return (int)ds.get(key);
    }

    public double getDouble(String key) {
        if (ds.containsKey(key))
            return (double)ds.get(key);
        else
            return 0;
    }

    public String toString() {
        return ds.toString();
    }
}
