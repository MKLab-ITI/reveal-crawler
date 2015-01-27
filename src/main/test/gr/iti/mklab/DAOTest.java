package gr.iti.mklab;

import gr.iti.mklab.queue.CrawlRequest;
import gr.iti.mklab.simmo.morphia.MorphiaManager;
import gr.iti.mklab.simmo.morphia.ObjectDAO;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.dao.DAO;
import org.mongodb.morphia.dao.BasicDAO;

import java.util.Date;


/**
 * Tests the Crawl DAO
 *
 * @author kandreadou
 */
public class DAOTest {


    private final static String DB_NAME = "testcrawl";
    @Before
    public void setUp() {
        MorphiaManager.setup(DB_NAME);
    }

    @After
    public void tearDown() {
        //MorphiaManager.getMongoClient().dropDatabase(DB_NAME);
        MorphiaManager.tearDown();
    }

    @Test
    public void test() {
        //testDAO();
    }

    public void testDAO(){
        DAO<CrawlRequest, ObjectId> crawlDAO = new BasicDAO<CrawlRequest, ObjectId>(CrawlRequest.class, MorphiaManager.getMongoClient(), MorphiaManager.getMorphia(), MorphiaManager.getDB("youtube").getName());
        CrawlRequest r = new CrawlRequest();
        r.collectionName = "new crawl";
        r.crawlDataPath = "/home/kandreadou/Documents/newcrawl/";
        r.creationDate = new Date(0);
        r.lastStateChange = new Date();
        r.portNumber = 9999;
        r.requestState = CrawlRequest.STATE.PAUSED;
        r.keywords.add("airplane");
        r.keywords.add("my keyword");
        crawlDAO.save(r);
        CrawlRequest r2 = crawlDAO.findOne("collectionName", r.collectionName);
        System.out.println(MorphiaManager.getMorphia().toDBObject(r2));
    }
}
