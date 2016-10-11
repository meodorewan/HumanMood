package algorithm;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import mongo.MongoModel;
import java.util.*;
import org.bson.Document;
import schemas.Schemas;

/**
 * Created by fx on 26/08/2016.
 */


class DocumentCmp implements Comparator<Document> {
    @Override
    public int compare(Document d1, Document d2) {
        return d1.getString(Schemas.NAME).compareTo(d1.getString(Schemas.NAME));
    }
}
public class Naivebayes {
    private List<Document> documents;

    private void initData(HttpServletRequest request) {
        MongoModel model = MongoModel.getInstance();

        String[] relatedAttributes = request.getParameter("data").split(",");
        String[] users = request.getParameter("users").split(",");

        documents = new ArrayList<Document>();

        for (String user : users) {
            //System.out.println(user);
            FindIterable<Document> iterable = model.getCollections(user);
            iterable.forEach(new Block<Document>() {
                @Override
                public void apply(Document document) {
                    documents.add(document);
                }
            });
        }
        Collections.sort(documents, new DocumentCmp());

        double prevTA = 0, prevEA = 0;
        //System.out.println(documents.size());

        for (int i = 0; i < documents.size(); i++){
            if (documents.get(i).containsKey(Schemas.MoodEA) == false)
                continue;
            if (documents.get(i).containsKey(Schemas.MoodTA) == false)
                continue;

            double tempTA = Double.parseDouble(documents.get(i).getString(Schemas.MoodTA));
            double tempEA = Double.parseDouble(documents.get(i).getString(Schemas.MoodEA));
            boolean before, after;

            if (i == 0 || diffTime(documents.get(i - 1), documents.get(i)) > 1 ||
                    documents.get(i - 1).containsKey(Schemas.MoodEA) == false)
                before = false;
            else
                before = true;

            if (i == documents.size() - 1 || diffTime(documents.get(i + 1), documents.get(i)) > 1 ||
                    documents.get(i + 1).containsKey(Schemas.MoodEA) == false)
                after = false;
            else
                after = true;
            if(before && after) {
                Document d = documents.get(i);
                Document dNext = documents.get(i + 1);
                double EANew = prevEA * 0.25 +
                        Double.parseDouble(d.getString(Schemas.MoodEA)) * 0.5 +
                        Double.parseDouble(dNext.getString(Schemas.MoodEA)) * 0.25;
                double TANew = prevTA * 0.25 +
                        Double.parseDouble(d.getString(Schemas.MoodTA)) * 0.5 +
                        Double.parseDouble(dNext.getString(Schemas.MoodTA)) * 0.25;

                d.replace(Schemas.MoodEA, (new Double(EANew)).toString());
                d.replace(Schemas.MoodTA, (new Double(TANew)).toString());
            } else if (before) {
                Document d = documents.get(i);
                double EANew = prevEA * 0.25 +
                        Double.parseDouble(d.getString(Schemas.MoodEA)) * 0.75;
                double TANew = prevTA * 0.25 +
                        Double.parseDouble(d.getString(Schemas.MoodTA)) * 0.75;

                d.replace(Schemas.MoodEA, (new Double(EANew)).toString());
                d.replace(Schemas.MoodTA, (new Double(TANew)).toString());
            } else if (after) {
                Document d = documents.get(i);
                Document dNext = documents.get(i + 1);
                double EANew = Double.parseDouble(d.getString(Schemas.MoodEA)) * 0.75 +
                        Double.parseDouble(dNext.getString(Schemas.MoodEA)) * 0.25;
                double TANew = Double.parseDouble(d.getString(Schemas.MoodTA)) * 0.75 +
                        Double.parseDouble(dNext.getString(Schemas.MoodTA)) * 0.25;

                d.replace(Schemas.MoodEA, (new Double(EANew)).toString());
                d.replace(Schemas.MoodTA, (new Double(TANew)).toString());
            }
            //System.out.println(documents.get(i).getString(Schemas.PRIMARY_KEY) + " " +
            //        tempEA + " " + tempTA + " " + before + " " + after
            //        + documents.get(i).getString(Schemas.MoodEA)
            //        + documents.get(i).getString(Schemas.MoodTA));
            prevEA = tempEA;
            prevTA = tempTA;
        }
    }

    private int diffTime(Document d, Document dNext) {
        String user_hour_id1 = d.getString(Schemas.PRIMARY_KEY);
        String user_hour_id2 = dNext.getString(Schemas.PRIMARY_KEY);
        //System.out.println(user_hour_id1 + " " + user_hour_id2);

        int c = 0;
        String y1="",m1="",d1="",h1="";
        for (int i = 0; i < user_hour_id1.length(); i++){
            if (user_hour_id1.charAt(i) == '_'){
                c++;
                continue;
            }
            if (c == 1) y1 += user_hour_id1.charAt(i);
            if (c == 2) m1 += user_hour_id1.charAt(i);
            if (c == 3) d1 += user_hour_id1.charAt(i);
            if (c == 4) h1 += user_hour_id1.charAt(i);
        }
        //System.out.println(user_hour_id1);
        //System.out.println(y1 + "/" + m1 + "/" + d1 + "/" + h1);
        int hour1 = Integer.parseInt(h1);
        int year1 = Integer.parseInt(y1);
        int month1 = Integer.parseInt(m1);
        int day1 = Integer.parseInt(d1);
        //System.out.println(year2 + " " + month2 + " " + day2 + " " + hour2);

        c = 0;
        String y2="",m2="",d2="",h2="";
        for (int i = 0; i < user_hour_id2.length(); i++){
            if (user_hour_id2.charAt(i) == '_'){
                c++;
                continue;
            }
            if (c == 1) y2 += user_hour_id2.charAt(i);
            if (c == 2) m2 += user_hour_id2.charAt(i);
            if (c == 3) d2 += user_hour_id2.charAt(i);
            if (c == 4) h2 += user_hour_id2.charAt(i);
        }
        int hour2 = Integer.parseInt(h2);
        int year2 = Integer.parseInt(y2);
        int month2 = Integer.parseInt(m2);
        int day2 = Integer.parseInt(d2);

        return Math.abs(year2 - year1) +
                Math.abs(month2 - month1) +
                Math.abs(day2 - day1) +
                Math.abs(hour2 - hour1);
    }
    public double getAccuracy(HttpServletRequest request) {
        initData(request);
        //System.out.println(documents.size());

        return 0;
    }
}
