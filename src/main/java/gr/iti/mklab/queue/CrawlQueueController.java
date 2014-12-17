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
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.util.*;

/**
 * Created by kandreadou on 12/16/14.
 */
public class CrawlQueueController {

    public static final String DB_NAME = "crawlerQUEUE";
    private static final int NUM_CRAWLERS = 1;
    private DAO<CrawlRequest, ObjectId> dao;
    private final static Integer[] AVAILABLE_PORTS = {9995, 9997, 9999};

    public CrawlQueueController() {
        MorphiaManager.setup(DB_NAME);
        dao = new BasicDAO<CrawlRequest, ObjectId>(CrawlRequest.class, MorphiaManager.getMongoClient(), MorphiaManager.getMorphia(), MorphiaManager.getDB().getName());
    }

    public void submit(String crawlDir, String collectionName) {

        enqueue(crawlDir, collectionName);
        tryLaunch();
    }

    public void cancel(int portNumber) {
        try {
            //LocateRegistry.createRegistry(9999);
            Map<String, Object> env = new HashMap<String, Object>();
            env.put("com.sun.management.jmxremote.authenticate", "false");
            env.put("com.sun.management.jmxremote.ssl", "false");
            //JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi://localhost/jndi/rmi://localhost:9999/jmxrmi");
            JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://127.0.0.1:" + portNumber + "/jmxrmi");
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

    private void enqueue(String crawlDir, String collectionName) {
        CrawlRequest r = new CrawlRequest();
        r.collectionName = collectionName;
        r.requestState = CrawlRequest.STATE.WAITING;
        r.lastStateChange = new Date();
        r.creationDate = new Date();
        r.crawlDataPath = crawlDir;
        dao.save(r);
    }

    private void tryLaunch() {
        List<CrawlRequest> list = getRunningCrawls();
        if (list.size() < NUM_CRAWLERS) {
            // Make a copy of the available port numbers
            List<Integer> ports = Arrays.asList(AVAILABLE_PORTS);
            // and find a non-used port
            for (CrawlRequest r : list) {
                if (ports.contains(r.portNumber))
                    ports.remove(r.portNumber);
            }
            for (Integer i : ports) {
                System.out.println("Launch crawl for port " + i);
                // Check if port is really available, if it is launch the respective script
                if (isPortAvailable(i)) {
                    launch("crawl" + i + ".sh");
                    break;
                }
            }


        }
    }

    private void launch(String scriptName) {
        try {
            String[] command = {"/bin/bash", scriptName};
            ProcessBuilder p = new ProcessBuilder(command);
            Process pr = p.start();
        } catch (IOException ioe) {
            System.out.println("Problem starting process for scriptName " + scriptName);
        }
    }

    private List<CrawlRequest> getRunningCrawls() {
        return dao.getDatastore().find(CrawlRequest.class).filter("requestState", CrawlRequest.STATE.RUNNING).asList();
    }

    private boolean isPortAvailable(int port) {

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                /* should not be thrown */
                }
            }
        }

        return false;

    }
}
