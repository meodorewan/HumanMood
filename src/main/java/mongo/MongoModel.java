package mongo;

import com.google.gson.Gson;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.opencsv.CSVReader;
import datastructure.DateTime;
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
 * MongoModel is the MODEL of framework
 */

public class MongoModel {
    private static final String DATABASE = "local";
    private static final String DBNAME = "log_user_behavior";
    private static MongoModel instance;
    private static MongoClient mongoClient;
    private static MongoDatabase db;

    public List<Document> cleanData;

    /**
     * MongoModel is unified and singleton
     * Always return an instance when called.
     * @return MongoModel
     */
    public static MongoModel getInstance() {
        if (instance == null) {
            instance = new MongoModel();
            mongoClient = new MongoClient();
            db = mongoClient.getDatabase(DATABASE);
        }
        return instance;
    }

    private MongoModel() {}

    /**
     * Load data to MODEL from text file
     * List files are being stored at "rescources/list-tables.txt"
     * Each file contains an unified format
     * @return void
     */
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

    /**
     * Insert one document to Model
     * Request should contains PRIMARY_KEY, and attributes
     * @param request
     */
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

    /**
     * FIND documents matching KEY and VALUE
     * @param key
     * @param value
     * @return
     */
    public List<Document> find(String key, String value) {
        FindIterable<Document> iterable = db.getCollection(DBNAME).find(new Document(key, value));
        final List<Document> result = new ArrayList<Document>();

        iterable.forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                result.add(document);
            }
        });
        return result;
    }

    /**
     * FIND ALL documents in Database
     * @return List<Document>
     */

    public List<Document> findAll() {
        FindIterable<Document> iterable = db.getCollection(DBNAME).find();
        final List<Document> result = new ArrayList<Document>();

        iterable.forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                if (document.containsKey(Schemas.MoodEA) && document.containsKey(Schemas.MoodTA))
                    result.add(document);
            }
        });

        return result;
    }

    /**
     * normalize model
     * Sort document by PRIMARY_KEY
     * Adjust EA,TA attributes by EA/TA formula
     * @return void
     */
    public void normalize() {
        cleanData = findAll();

        Comparator<Document> comparator = new Comparator<Document>() {
            @Override
            public int compare(Document o1, Document o2) {
                return o1.getString(Schemas.PRIMARY_KEY).compareTo(
                        o2.getString(Schemas.PRIMARY_KEY)
                );
            }
        };

        Collections.sort(cleanData, comparator);
        List<Document> temp = new ArrayList<Document>();

        for (int i = 0; i < cleanData.size(); i++) {
            Document previousDocument, currentDocument, nextDocuement;
            previousDocument = (i == 0) ? null : cleanData.get(i - 1);
            currentDocument = cleanData.get(i);
            nextDocuement = (i == cleanData.size() - 1) ? null : cleanData.get(i + 1);

            currentDocument.append("EA", adjustEA(previousDocument, currentDocument, nextDocuement));
            currentDocument.append("TA", adjustTA(previousDocument, currentDocument, nextDocuement));
            temp.add(currentDocument);
            //System.out.println(currentDocument.getDouble("EA") + " " + currentDocument.getDouble("TA"));
        }
        cleanData = temp;
    }
    private double adjustEA(Document previousDocument, Document currentDocument, Document nextDocument) {
        double prevMood = (previousDocument == null) ? 0 : Double.parseDouble(previousDocument.getString(Schemas.MoodEA));
        double currMood = (currentDocument == null) ? 0 : Double.parseDouble(currentDocument.getString(Schemas.MoodEA));
        double nextMood = (nextDocument == null) ? 0 : Double.parseDouble(nextDocument.getString(Schemas.MoodEA));

        if (continuous(previousDocument, currentDocument) == -1 && continuous(currentDocument, nextDocument) == -1)
            return currMood;
        else if (continuous(previousDocument, currentDocument) == -1)
            return currMood * 0.5 + nextMood * 0.5;
        else {
            if (continuous(currentDocument, nextDocument) == -1)
                return currMood * 0.5 + prevMood * 0.5;
            else {
                double k1 = continuous(previousDocument, currentDocument);
                double k2 = continuous(currentDocument, nextDocument);
                return prevMood * k1 / (2 * (k1 + k2)) + currMood * 0.5 + nextMood * k2 / (2 * (k1 + k2));
            }
        }
    }

    private double adjustTA(Document previousDocument, Document currentDocument, Document nextDocument) {
        double prevMood = (previousDocument == null) ? 0 : Double.parseDouble(previousDocument.getString(Schemas.MoodEA));
        double currMood = (currentDocument == null) ? 0 : Double.parseDouble(currentDocument.getString(Schemas.MoodEA));
        double nextMood = (nextDocument == null) ? 0 : Double.parseDouble(nextDocument.getString(Schemas.MoodEA));
        if (continuous(previousDocument, currentDocument) == -1 && continuous(currentDocument, nextDocument) == -1)
            return currMood;
        else if (continuous(previousDocument, currentDocument) == -1)
            return currMood * 0.5 + nextMood * 0.5;
        else {
            if (continuous(currentDocument, nextDocument) == -1)
                return currMood * 0.5 + prevMood * 0.5;
            else {
                double k1 = continuous(previousDocument, currentDocument);
                double k2 = continuous(currentDocument, nextDocument);
                return prevMood * k1 / (2 * (k1 + k2)) + currMood * 0.5 + nextMood * k2 / (2 * (k1 + k2));
            }
        }
    }

    private double continuous(Document previousDocument, Document currentDocument) {
        if (previousDocument == null || currentDocument == null)
            return -1;
        if (!previousDocument.get(Schemas.NAME).equals(
             currentDocument.get(Schemas.NAME)))
            return -1;

        DateTime prevTime = extractDateTime(previousDocument);
        DateTime currTime = extractDateTime(currentDocument);
        if (prevTime.year != currTime.year)
            return -1;
        if (prevTime.month != currTime.month)
            return -1;
        if (prevTime.day != currTime.day)
            return -1;
        return Math.abs(currTime.hour - prevTime.hour);
    }

    /**
     * EXTRACT DateTime of a document
     * @param document
     * @return DateTime
     */
    public DateTime extractDateTime(Document document) {
        String key = document.getString(Schemas.PRIMARY_KEY);

        //key has NAME_year_month_day_hour format
        int numOfUnderscore = 0;
        int year = 0, month = 0, day = 0, hour = 0;
        for (int i = 0; i < key.length(); i++) {
            if (key.charAt(i) == '_'){
                numOfUnderscore++;
                continue;
            }
            if (numOfUnderscore == 1)
                year = year*10 + key.charAt(i) - 48;
            if (numOfUnderscore == 2)
                month = month*10 + key.charAt(i) - 48;
            if (numOfUnderscore == 3)
                day = day*10 + key.charAt(i) - 48;
            if (numOfUnderscore == 4)
                hour = hour*10 + key.charAt(i) - 48;
        }
        return new DateTime(year, month, day, hour);
    }
}
