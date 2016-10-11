import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.opencsv.CSVReader;
import org.bson.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Array;
import java.util.Scanner;
import java.util.ArrayList;

/**
 * Created by fx on 03/08/2016.
 */

public class Seed {

    public static void main(String[] args) {
        MongoClient mongoClient = new MongoClient();
        MongoDatabase db = mongoClient.getDatabase("local");

        CSVReader reader = null;
        db.getCollection("log_user_behavior").drop();

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

                        db.getCollection("log_user_behavior").insertOne(document);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
