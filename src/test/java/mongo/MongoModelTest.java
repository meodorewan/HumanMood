package mongo;

import junit.framework.TestCase;
import org.bson.Document;
import schemas.Schemas;

import javax.validation.constraints.AssertTrue;

/**
 * Created by fx on 12/10/2016.
 */
public class MongoModelTest extends TestCase {

    public void testGetTimestamp() throws Exception {
        Document test1 = new Document();
        Document test2 = new Document();

        test1.append(Schemas.PRIMARY_KEY, "DONG_2016_08_13_16");
        test2.append(Schemas.PRIMARY_KEY, "EX_2007_04_02_11");

        MongoModel instance = MongoModel.getInstance();
        assertEquals(instance.getTimestamp(test1), 17414944);
        assertEquals(instance.getTimestamp(test2), 17334035);
    }


}