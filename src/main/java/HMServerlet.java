import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import algorithm.Naivebayes;

import Result.HMResult;
import mongo.MongoModel;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by fx on 05/08/2016.
 */
public class HMServerlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String command = request.getParameter("command");
        //System.out.println(command);
        HMResult result = new HMResult();
        switch (command) {
            case "update":
                result = update(request);
                break;
            case "k-nearest-neighbor":
                result = knearestneighbor(request);
                break;
            case "naive-bayes":
                result = naivebayes(request);
                break;
        }
        PrintWriter out = response.getWriter();
        out.print(result);
    }

    private HMResult naivebayes(HttpServletRequest request) {
        HMResult result = new HMResult();
        result.add((new Naivebayes()).getAccuracy(request));
        return result;
    }

    private HMResult knearestneighbor(HttpServletRequest request) {
        return null;
    }

    private HMResult update(HttpServletRequest request) {
        HMResult result = new HMResult();

        try {
            MongoModel.getInstance().insertOneCollection(request);
        } catch (Exception e) {
            result.add(e.getMessage());
        }
        return result;
    }
}
