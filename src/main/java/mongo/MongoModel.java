package mongo;

import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.opencsv.CSVReader;
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

    public FindIterable<Document> getCollections(String username) {
        return db.getCollection(DBNAME).find(
                new Document(Schemas.NAME, username)
        );
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
    public void insertOneCollection(HttpServletRequest request) {
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
        System.out.println(name);

        //add more features to username
        //System.out.println("updating data");
        //System.out.println(request.getParameter("data"));

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
}
