package gr.iti.mklab.queue;

import gr.iti.mklab.simmo.morphia.MorphiaManager;
import org.bson.types.ObjectId;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.dao.DAO;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kandreadou on 12/16/14.
 */
public class CrawlQueueController {

    public static final String DB_NAME = "crawlerQUEUE";
    private static final int NUM_CRAWLERS = 1;
    private DAO<CrawlRequest, ObjectId> dao;

    public CrawlQueueController() {
        MorphiaManager.setup(DB_NAME);
        dao = new BasicDAO<CrawlRequest, ObjectId>(CrawlRequest.class, MorphiaManager.getMongoClient(), MorphiaManager.getMorphia(), MorphiaManager.getDB().getName());
    }

    public void submit(int portNumber, String crawlDir, String collectionName) {
        try {
            enqueue(portNumber, crawlDir, collectionName);
            if (hasEmptySlots())
                launch();
        } catch (IOException ioe) {
            System.out.println("IOException when launching crawl " + ioe);
        }
    }

    public void cancel(int portNumber) {
        try {
            //LocateRegistry.createRegistry(9999);
            Map<String,Object> env = new HashMap<String,Object>();
            env.put("com.sun.management.jmxremote.authenticate", "false");
            env.put("com.sun.management.jmxremote.ssl", "false");
            //JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi://localhost/jndi/rmi://localhost:9999/jmxrmi");
            JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://127.0.0.1:9999/jmxrmi");
            JMXConnector cc = JMXConnectorFactory.connect(jmxServiceURL, env);
            MBeanServerConnection mbsc = cc.getMBeanServerConnection();
            //This information is available in jconsole
            ObjectName serviceConfigName = new ObjectName("it.unimi.di.law.bubing:type=Agent,name=agent");
            //  Invoke stop operation
            mbsc.invoke(serviceConfigName, "stop", null, null);
            //  Close JMX connector
            cc.close();
        } catch (Exception e) {
            System.out.println("Exception occurred: " + e.toString());
            e.printStackTrace();
        }
    }

    private void enqueue(int portNumber, String crawlDir, String collectionName) {
        CrawlRequest r = new CrawlRequest();
        r.collectionName = collectionName;
        r.requestState = CrawlRequest.STATE.WAITING;
        r.portNumber = portNumber;
        r.lastStateChange = new Date();
        r.creationDate = new Date();
        r.crawlDataPath = crawlDir;
        dao.save(r);
    }

    private boolean hasEmptySlots() {
        return dao.getDatastore().find(CrawlRequest.class).filter("requestState", CrawlRequest.STATE.RUNNING).asList().size() < NUM_CRAWLERS;
    }

    private void launch() throws IOException {
        String[] command = {"/bin/bash", "crawl.sh"};
        ProcessBuilder p = new ProcessBuilder(command);
        Process pr = p.start();
    }
}
