package datastructure;

import java.util.*;

/**
 * Created by fx on 12/10/2016.
 */
public class HMDataStructure {
    private Map<String, Object> ds;

    public HMDataStructure() {
        ds = new HashMap<String, Object>();
    }

    public void add(String key, double value) {
        ds.put(key, value);
    }

    public void add(String key, String value) {
        ds.put(key, value);
    }

    public int get(String timestamp) {
        return (int)ds.get(timestamp);
    }
}
