import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import algorithm.Naivebayes;

import Result.HMResult;
import datastructure.HMDataStructure;
import mongo.MongoModel;

import java.io.IOException;
import java.io.PrintWriter;
import com.google.gson.*;
import java.util.*;

import mongo.MongoModel;
import org.bson.Document;
import schemas.Schemas;

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
            case "correlation":
                try {
                    result = correlation(request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

        }
        PrintWriter out = response.getWriter();
        out.print(result);
    }

    private HMResult correlation(HttpServletRequest request) throws Exception {
        Map<String,String> params = new Gson().fromJson(request.getParameter("data"), Map.class);
        HMResult result = new HMResult();

        String username = params.get("username");
        if (username == null)
            throw new Exception("username param is not found");

        String firstParam = params.get("first");
        String secondParam = params.get("second");
        if (firstParam == null || secondParam == null)
            throw new Exception("first or second param is not found");

        List<HMDataStructure> adjustedData = MongoModel.getInstance().adjust(username, firstParam, secondParam);
        return result;
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
            MongoModel.getInstance().insertOne(request);
        } catch (Exception e) {
            result.add(e.getMessage());
        }
        return result;
    }
}
