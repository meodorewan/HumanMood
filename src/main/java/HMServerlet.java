import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import algorithm.KNearestNeighbor;
import algorithm.Naivebayes;

import algorithm.Pearson;
import com.google.gson.Gson;
import org.bson.Document;
import result.HMResult;
import mongo.MongoModel;
import schemas.Schemas;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.*;
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
            case "normalize":
                result = normalize(request);
                break;
            case "pearson":
                result = pearson(request);
                break;
            case "export":
                exportData(request);
                break;
        }
        PrintWriter out = response.getWriter();
        out.print(result);
    }

    private void exportData(HttpServletRequest request) throws FileNotFoundException, UnsupportedEncodingException {
        MongoModel model = MongoModel.getInstance();
        model.normalize();

        PrintWriter writer = new PrintWriter("resources/clean-data.txt", "UTF-8");
        for (int i = 0; i < model.cleanData.size(); i++){
            Document document = model.cleanData.get(i);
            double hr_avg = Double.parseDouble(document.getString(Schemas.HR_avg));
            if (hr_avg == 0)
                continue;
            writer.print(document.toString());
        }
        writer.close();
    }

    private HMResult pearson(HttpServletRequest request) {
        Map<String,String> data = new Gson().fromJson(request.getParameter("data"), Map.class);
        String user = data.get("user");

        HMResult result = new HMResult();
        if (user == null) {
            result.add("Error, Pearson must specify username");
            return result;
        }

        String firstAttribute = data.get("attribute-1");
        String secondAttribute = data.get("attribute-2");


        if (firstAttribute == null || secondAttribute == null){
            result.add("Error, Pearson must specify username");
            return result;
        }
        result.add(Pearson.correlation(user, firstAttribute, secondAttribute));
        return result;
    }

    private HMResult normalize(HttpServletRequest request) {
        HMResult result = new HMResult();
        try {
            MongoModel.getInstance().normalize();
            result.add("train model successfully!!!");
        } catch (Exception e){
            result.add("train model unsuccessfully");
            result.add(e.getMessage());
        }
        return result;
    }

    private HMResult naivebayes(HttpServletRequest request) {
        Map<String,String> data = new Gson().fromJson(request.getParameter("data"), Map.class);
        String user = data.get("user");
        String prediction = data.get("prediction");
        int numClusters = Integer.parseInt(data.get("num_clusters"));
        double ratio;

        if (data.get("ratio") == null)
            ratio = 0.7;
        else
            ratio = Double.parseDouble(data.get("ratio"));

        String parameters[] = data.get("parameters").split(",");

        HMResult result = new HMResult();

        Naivebayes naiveBayes = new Naivebayes(user, prediction, numClusters);
        naiveBayes.train();
        result.add(naiveBayes.getAccuracy(ratio, parameters));

        return result;
    }

    private HMResult knearestneighbor(HttpServletRequest request) {
        Map<String,String> data = new Gson().fromJson(request.getParameter("data"), Map.class);
        String user = data.get("user");
        String prediction = data.get("prediction");
        int numClusters = Integer.parseInt(data.get("num_clusters"));
        double ratio;
        String[] parameters = data.get("parameters").split(",");

        //ratio default 0.7
        if (data.get("ratio") == null)
            ratio = 0.7;
        else
            ratio = Double.parseDouble(data.get("ratio"));

        HMResult result = new HMResult();

        if (user == null || prediction == null){
            result.add("Error, k-nearest-neighbor needs to specify user and prediction");
            return result;
        }

        KNearestNeighbor kNearestNeighbor = new KNearestNeighbor(user, prediction, numClusters);
        kNearestNeighbor.train();
        result.add(kNearestNeighbor.getAccuracy(ratio, parameters));
        return result;
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
