package algorithm;

import mongo.MongoModel;
import org.bson.Document;
import schemas.Schemas;

import java.util.*;

import static java.lang.Math.sqrt;

/**
 * Created by fx on 22/10/2016.
 */
public class Pearson {
    public static double correlation(String user, String firstAttribute, String secondAttribute) {
        MongoModel model = MongoModel.getInstance();

        model.normalize();
        List<Document> documents = model.cleanData;

        int n = 0;
        double sumXY = 0, sumX = 0, sumY = 0, sqrSumX = 0, sqrSumY = 0;


        for (int i = 0; i < documents.size(); i++){
            Document document = documents.get(i);

            if (!user.equals(document.get(Schemas.NAME)))
                continue;
            if (!document.containsKey(firstAttribute) || !document.containsKey(secondAttribute))
                continue;

            double X = Double.parseDouble(document.getString(firstAttribute));
            double Y = Double.parseDouble(document.getString(secondAttribute));
            if (X == 0 || Y == 0)
                continue;
            n++;
            sumXY += X * Y;
            sumX += X;
            sumY += Y;
            sqrSumX += X * X;
            sqrSumY += Y * Y;
        }
        double A = (sumXY / n) - (sumX/n)*(sumY/n);
        double B = sqrt(sqrSumX/n - sumX*sumX / (n * n)) * sqrt(sqrSumY/n - sumY*sumY / (n * n));
        return A/B;
    }
}
