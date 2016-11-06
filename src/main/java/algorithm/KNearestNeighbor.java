package algorithm;

import datastructure.HMDataStructure;
import mongo.MongoModel;
import org.bson.Document;
import schemas.Schemas;

import java.util.*;

/**
 * Created by fx on 21/10/2016.
 */
public class KNearestNeighbor {
    private final String user;
    private final String prediction;
    private final int numClusters;
    private List<Document> data;

    public KNearestNeighbor(String user, String parameter, int numClus) {
        this.user = user;
        this.prediction = parameter;
        this.numClusters = numClus;
    }

    public double getAccuracy(double ratio, String[] parameters) {
        Collections.shuffle(data);
        double[] mean = new double[parameters.length];

        //System.out.println(ratio + " " + parameters[0] + " " + parameters[1] + " " + parameters[2]);

        //normalize
        for (int i = 0; i < parameters.length; i++) {
            mean[i] = 0;
            for (int j = 0; j < data.size(); j++) {
                if (data.get(j).getString(parameters[i]) == null)
                    mean[i] += 0;
                else
                    mean[i] += Double.parseDouble(data.get(j).getString(parameters[i]));
            }
            mean[i] /= data.size();
        }

        HMDataStructure[] normalization = new HMDataStructure[data.size()];
        List<HMDataStructure> trainingSet = new ArrayList<HMDataStructure>();
        List<HMDataStructure> testingSet = new ArrayList<HMDataStructure>();

        for (int i = 0; i < data.size(); i++)
            normalization[i] = new HMDataStructure();
        for (int i = 0; i < parameters.length; i++) {
            for (int j = 0; j < data.size(); j++){
                double value;
                if (data.get(j).getString(parameters[i]) == null)
                    value = 0;
                else
                    value = Double.parseDouble(data.get(j).getString(parameters[i]));

                normalization[j].add(parameters[i], value);
            }
        }
        //assign label
        for (int j = 0; j < data.size(); j++)
            normalization[j].add("label", data.get(j).getInteger("label"));

        for (int i = 0; i < normalization.length; i++)
            if (i < normalization.length * ratio)
                trainingSet.add(normalization[i]);
            else
                testingSet.add(normalization[i]);

        //computer euclid distance
        //compute accuracy
        int n_true = 0;
        int n_predict = testingSet.size();
        for (int i = 0; i < testingSet.size(); i++){
            int best = 0;
            for (int j = 1; j < trainingSet.size(); j++) {
                if (computeDistance(testingSet.get(i), trainingSet.get(j), parameters) <
                    computeDistance(testingSet.get(i), trainingSet.get(best), parameters))
                    best = j;
            }
            //System.out.println(trainingSet.get(best));
            int predict = trainingSet.get(best).getInt("label");
            if (predict == testingSet.get(i).getInt("label"))
                n_true++;
        }
        System.out.println(n_true + " " + n_predict);
        return (double)n_true/n_predict;
    }

    private double computeDistance(HMDataStructure o1, HMDataStructure o2, String[] parameters) {
        double result =0;
        for (int i = 0; i < parameters.length; i++){
            double a, b;
            a = o1.getDouble(parameters[i]);
            b = o2.getDouble(parameters[i]);
            result += Math.pow(a - b, 2);
        }
        return Math.sqrt(result);
    }

    /**
     * Train model, label input
     */

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
}
