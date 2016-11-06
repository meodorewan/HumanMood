package result;

import java.util.*;

/**
 * Created by fx on 15/08/2016.
 */
public class HMResult {
    private List<Object> array = new ArrayList<Object>();

    public void add(Object message) {
        array.add(message);
    }

    public String toString() {
        String result = "";
        for (int i = 0; i < array.size(); i++)
            result += array.get(i).toString() + "\n";
        return result;
    }
}
