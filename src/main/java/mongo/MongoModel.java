package mongo;

import com.google.gson.Gson;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.opencsv.CSVReader;
import datastructure.HMDataStructure;
import org.bson.Document;
import schemas.Schemas;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.geoIntersects;

/**
 * Created by fx on 05/08/2016.
 *
 * MongoModel receive data from text file, update frequently
 *
 */

public class MongoModel {
    private static final String DATABASE = "local";
    private static final String DBNAME = "log_user_behavior";
    private static MongoModel instance;
    private static MongoClient mongoClient;
    private static MongoDatabase db;

    //singleton
    public static MongoModel getInstance() {
        if (instance == null) {
            instance = new MongoModel();
            mongoClient = new MongoClient();
            db = mongoClient.getDatabase(DATABASE);
        }
        return instance;
    }

    public void reloadModel() {
        MongoClient mongoClient = new MongoClient();
        MongoDatabase db = mongoClient.getDatabase(DATABASE);

        CSVReader reader = null;
        db.getCollection(DBNAME).drop();

        try {
            ArrayList<String> listTables = new ArrayList<String>();
            Scanner fsc = new Scanner(new File("resources/list-tables.txt"));
            while (fsc.hasNext())
                listTables.add(fsc.next());

            for (String filename: listTables){
                reader = new CSVReader(new FileReader("resources/" + filename));
                try {
                    String[] schemas = reader.readNext();
                    String[] line;

                    while ((line = reader.readNext()) != null) {
                        Document document = new Document();
                        for (int i = 0; i < line.length; i++)
                            document.append(schemas[i], line[i].replace(',', '.'));

                        db.getCollection(DBNAME).insertOne(document);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void insertOne(HttpServletRequest request) {
        HashMap<String, String> params = (new Gson()).fromJson(request.getParameter("data"),
                (new HashMap()).getClass());
        Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();

        Document document = new Document();

        while (it.hasNext()) {
            HashMap.Entry pair = it.next();
            document.append(String.valueOf(pair.getKey()), pair.getValue());
        }

        String name = "";
        if (document.containsKey(Schemas.PRIMARY_KEY)){
            String content = document.get(Schemas.PRIMARY_KEY).toString();
            for (int i = 0; i < content.length(); i++) {
                if (content.charAt(i) == '_') break;
                name += content.charAt(i);
            }
        }
        document.append(Schemas.NAME, name);

        FindIterable<Document> iterable = db.getCollection(DBNAME).find(
                new Document(Schemas.PRIMARY_KEY, document.getString(Schemas.PRIMARY_KEY))
        );

        if (iterable.first() != null) {
            System.out.println(iterable.first().toJson().toString());

            db.getCollection(DBNAME).updateOne(
                    eq(Schemas.PRIMARY_KEY, document.getString(Schemas.PRIMARY_KEY)),
                    new Document("$set", document)
            );
        } else {
            System.out.println("not exist, insert one");
            db.getCollection(DBNAME).insertOne(document);
        }
    }
    /*
    * find all document matching schema with target
    */
    public List<Document> find(String schema, String target) {
        FindIterable<Document> iterable = db.getCollection(DBNAME).find(new Document(schema, target));
        final List<Document> result = new ArrayList<Document>();

        iterable.forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                result.add(document);
            }
        });
        return result;
    }


    public List<HMDataStructure> adjust(String username, String firstParam, String secondParam) throws Exception {
        List<Document> docs = find(Schemas.NAME, username);
        List<HMDataStructure> result = new ArrayList<HMDataStructure>();

        if (!firstParam.equals("MoodEA") && !firstParam.equals("MoodTA"))
            throw new Exception("first params should be MoodEA or MoodTA");

        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            String n = doc.getString(Schemas.NAME);
            if (doc.getString(firstParam) == null ||
                    doc.getString(secondParam) == null)
                throw new Exception("first param or second param is not found in database");

            double mood = Double.parseDouble(doc.getString(firstParam));
            double condition = Double.parseDouble(doc.getString(secondParam));

            HMDataStructure nw = new HMDataStructure();
            nw.add(Schemas.NAME, n);
            nw.add(firstParam, mood);
            nw.add(secondParam, condition);
            nw.add("timestamp", getTimestamp(doc));
            result.add(nw);
        }

        Comparator<HMDataStructure> comparator = new Comparator<HMDataStructure>() {
            @Override
            public int compare(HMDataStructure o1, HMDataStructure o2) {
                return o2.get("timestamp") - o1.get("timestamp");
            }
        };
        Collections.sort(result, comparator);
        return result;
    }
    /*
     * timestamp is counted in hour
     * 1 day = 24 hours
     * 1 month = 30 days average
     * 1 year = 12 months
    */
    public long getTimestamp(Document doc) {
        String date = doc.getString(Schemas.PRIMARY_KEY);
        int c = 0;
        int year = 0;
        int month = 0;
        int day = 0;
        int hour = 0;
        for (int i = 0; i < date.length(); i++) {
            if (date.charAt(i) == '_') {
                c++;
                continue;
            }
            if (c == 1) year = year * 10 + date.charAt(i) - 48;
            if (c == 2) month = month * 10 + date.charAt(i) - 48;
            if (c == 3) day = day * 10 + date.charAt(i) - 48;
            if (c == 4) hour = hour * 10 + date.charAt(i) - 48;
        }
        //System.out.println(year + " " + month + " " + day + " " + hour);
        return (year - 1) * 12 * 30 * 24 + (month - 1) * 30 * 24 + (day - 1) * 24 + hour;
    }
}
