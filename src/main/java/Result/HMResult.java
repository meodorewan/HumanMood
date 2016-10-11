package Result;

import java.util.*;

/**
 * Created by fx on 15/08/2016.
 */
public class HMResult {
    private List array = new ArrayList<String>();
    private double accuracy;

    public void add(String message) {
        array.add(message);
    }

    public void add(double acc) {
        accuracy = acc;
    }
}
