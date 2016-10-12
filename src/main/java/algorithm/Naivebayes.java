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
    public double getAccuracy(HttpServletRequest request) {
        //System.out.println(documents.size());

        return 0;
    }
}
