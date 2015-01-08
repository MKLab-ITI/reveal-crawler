package gr.iti.mklab.bubing;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.martiansoftware.jsap.*;
import gr.iti.mklab.bubing.parser.ITIHTMLParser;
import gr.iti.mklab.image.VisualIndexer;
import gr.iti.mklab.queue.CrawlQueueController;
import gr.iti.mklab.queue.CrawlRequest;
import gr.iti.mklab.simmo.morphia.MorphiaManager;
import it.unimi.di.law.bubing.Agent;
import it.unimi.di.law.bubing.RuntimeConfiguration;
import it.unimi.di.law.bubing.StartupConfiguration;
import org.apache.commons.configuration.BaseConfiguration;
import org.bson.types.ObjectId;
import org.mongodb.morphia.dao.DAO;
import org.mongodb.morphia.dao.BasicDAO;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

/**
 * An ItiAgent which initializes the VisualIndexer before starting the BUbiNG Agent
 *
 * @author Katerina Andreadou
 */
public class ItiAgent {

    public final static BloomFilter<String> UNIQUE_IMAGE_URLS = BloomFilter.create(Funnels.stringFunnel(Charset.forName("UTF-8")), 100000);
    public static final String JMX_REMOTE_PORT_SYSTEM_PROPERTY = "com.sun.management.jmxremote.port";


    public static void main(final String arg[]) throws Exception {
        MorphiaManager.setup(CrawlQueueController.DB_NAME);
        DAO<CrawlRequest, ObjectId> dao = new BasicDAO<CrawlRequest, ObjectId>(CrawlRequest.class, MorphiaManager.getMongoClient(), MorphiaManager.getMorphia(), MorphiaManager.getDB().getName());
        List<CrawlRequest> waitingRequests = dao.getDatastore().find(CrawlRequest.class).filter("requestState", CrawlRequest.STATE.WAITING).order("creationDate").asList();
        if (waitingRequests.size() == 0) {
            System.out.println("No waiting requests in queue");
            return;
        } else {
            System.out.println("Keywords in request");
            CrawlRequest req = waitingRequests.get(0);
            for(String k:req.keywords){
                System.out.println(k);
            }
            String crawlPath = req.crawlDataPath;
            String collection = req.collectionName;

            SimpleJSAP jsap = new SimpleJSAP(Agent.class.getName(), "Starts a BUbiNG agent (note that you must enable JMX by means of the standard Java system properties).",
                    new Parameter[]{
                            new FlaggedOption("weight", JSAP.INTEGER_PARSER, "1", JSAP.NOT_REQUIRED, 'w', "weight", "The agent weight."),
                            new FlaggedOption("group", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'g', "group", "The JGroups group identifier (must be the same for all cooperating agents)."),
                            new FlaggedOption("jmxHost", JSAP.STRING_PARSER, InetAddress.getLocalHost().getHostAddress(), JSAP.REQUIRED, 'h', "jmx-host", "The IP address (possibly specified by a host name) that will be used to expose the JMX RMI connector to other agents."),
                            new FlaggedOption("rootDir", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'r', "root-dir", "The root directory."),
                            new Switch("new", 'n', "new", "Start a new crawl"),
                            new FlaggedOption("properties", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'P', "properties", "The properties used to configure the agent."),
                            new UnflaggedOption("name", JSAP.STRING_PARSER, JSAP.REQUIRED, "The agent name (an identifier that must be unique across the group).")
                    });

            JSAPResult jsapResult = jsap.parse(arg);
            if (jsap.messagePrinted()) System.exit(1);

            // JMX *must* be set up.
            final String portProperty = System.getProperty(JMX_REMOTE_PORT_SYSTEM_PROPERTY);
            if (portProperty == null)
                throw new IllegalArgumentException("You must specify a JMX service port using the property " + JMX_REMOTE_PORT_SYSTEM_PROPERTY);

            final String name = jsapResult.getString("name");
            final int weight = jsapResult.getInt("weight");
            final String group = jsapResult.getString("group");
            final String host = jsapResult.getString("jmxHost");
            final int port = Integer.parseInt(portProperty);
            final String rootDir = jsapResult.getString("rootDir");

            req.requestState = CrawlRequest.STATE.RUNNING;
            req.lastStateChange = new Date();
            req.portNumber = port;
            System.out.println("ItiAgent port number "+port);
            dao.save(req);

            BaseConfiguration additional = new BaseConfiguration();
            additional.addProperty("name", name);
            additional.addProperty("group", group);
            additional.addProperty("weight", Integer.toString(weight));
            additional.addProperty("crawlIsNew", Boolean.valueOf(jsapResult.getBoolean("new")));
            //if (jsapResult.userSpecified("rootDir")) additional.addProperty("rootDir", rootDir);
            //NOTE: This is new
            additional.addProperty("rootDir", crawlPath);
            ITIHTMLParser.keywords = req.keywords;
            VisualIndexer.createInstance(collection);
            new Agent(host, port, new RuntimeConfiguration(new StartupConfiguration(jsapResult.getString("properties"), additional)));
            req.requestState = CrawlRequest.STATE.FINISHED;
            req.lastStateChange = new Date();
            dao.save(req);
            MorphiaManager.tearDown();
            System.exit(0); // Kills remaining FetchingThread instances, if any.
        }
    }
}
