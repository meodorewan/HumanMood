package algorithm;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.MalformedJsonException;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import datastructure.HMDataStructure;
import mongo.MongoModel;
import java.util.*;
import org.bson.Document;
import schemas.Schemas;

public class Naivebayes {
    String user, prediction;
    int numClusters;
    private List<Document> data;

    public Naivebayes(String user, String prediction, int numClusters) {
        this.user = user;
        this.prediction = prediction;
        this.numClusters = numClusters;
    }

    public void train() {
        MongoModel model = MongoModel.getInstance();
        model.normalize();
        data = model.cleanData;

        List<Document> temp = new ArrayList<Document>();

        for (int i = 0; i < data.size(); i++) {
            Document document = data.get(i);
            if (document.get(Schemas.NAME).equals(user))
                temp.add(document);
        }
        data = temp;

        for (int i = 0; i < data.size(); i++) {
            Document document = data.get(i);
            double mood = document.getDouble(prediction);
            document.append("label", label(mood) + 1);
            data.set(i, document);
        }
    }

    private int label(double mood) {
        double distance = 4.0 / numClusters;
        if (mood == 0) return 0;
        return (int)Math.floor((mood - 0.1) / distance);
    }

    public Double getAccuracy(double ratio, String[] parameters) {
        double P[] = new double[8];
        double Nk[] = new double[8];
        double Variance[][] = new double[8][parameters.length];
        double Median[][] = new double[8][parameters.length];

        Collections.shuffle(data);
        List<HMDataStructure> trainningSet = new ArrayList<HMDataStructure>();
        List<HMDataStructure> testingSet = new ArrayList<HMDataStructure>();

        for (int i = 0; i < data.size(); i++){
            HMDataStructure fur = new HMDataStructure();
            for (int j = 0; j < parameters.length; j++)
                if (data.get(i).getString(parameters[j]) != null)
                    fur.add(parameters[j], Double.parseDouble(data.get(i).getString(parameters[j])));
                else
                    fur.add(parameters[j], 0.0);
            fur.add("label", data.get(i).getInteger("label"));
            if (i < data.size() * ratio)
                trainningSet.add(fur);
            else
                testingSet.add(fur);
        }
        // calculate Nk, P arrays
        for (int i = 0; i < trainningSet.size(); i++)
            Nk[trainningSet.get(i).getInt("label")]++;

        for (int i = 1; i <= numClusters; i++)
            P[i] = Nk[i] / trainningSet.size();


        //System.out.println("yes");
        //calculate the median of parameter i
        for (int i = 0; i < parameters.length; i++)
            for (int j = 0; j < trainningSet.size(); j++) {
                int label = trainningSet.get(j).getInt("label");
                Median[label][i] += trainningSet.get(j).getDouble(parameters[i]);
            }

        for (int i = 1; i <= numClusters; i++)
            for (int j = 0; j < parameters.length; j++)
                Median[i][j] /= Nk[i];

        //calculate the variance of parameter i
        for (int i = 0; i < parameters.length; i++)
            for (int j = 0; j < trainningSet.size(); j++)
                Variance[trainningSet.get(j).getInt("label")][i] +=
                        sqr(trainningSet.get(j).getDouble(parameters[i]) - Median[trainningSet.get(j).getInt("label")][i]);

        //predict
        int n_true = 0;
        for (int x = 0; x < testingSet.size(); x++){
            HMDataStructure bar = testingSet.get(x);
            double best = 0;
            int y_predict = 0;
            for (int i = 1; i <= numClusters; i++) {
                double res = 1;
                for (int j = 0; j < parameters.length; j++) {
                    double Xi = bar.getDouble(parameters[j]);
                    double a = Math.sqrt(2 * Math.PI * Variance[i][j]); //it should actually be 1/a
                    double exp = -sqr(Xi - Median[i][j]) / (2 * Variance[i][j]);
                    res *= Math.pow(Math.E, exp) / a;
                }
                if (res < best) {
                    best = res;
                    y_predict = i;
                }
            }
            if (y_predict == bar.getInt("label"))
                n_true++;
        }
        return (double)n_true / testingSet.size();
    }

    private double sqr(double v) {
        return v * v;
    }
}
