package gr.iti.mklab.bubing;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.martiansoftware.jsap.*;
import gr.iti.mklab.image.VisualIndexer;
import it.unimi.di.law.bubing.Agent;
import it.unimi.di.law.bubing.RuntimeConfiguration;
import it.unimi.di.law.bubing.StartupConfiguration;
import org.apache.commons.configuration.BaseConfiguration;

import java.net.InetAddress;
import java.nio.charset.Charset;

/**
 * An ItiAgent which initializes the VisualIndexer before starting the BUbiNG Agent
 *
 * @author Katerina Andreadou
 */
public class ItiAgent {

    public final static BloomFilter<String> UNIQUE_IMAGE_URLS = BloomFilter.create(Funnels.stringFunnel(Charset.forName("UTF-8")), 100000);
    public static final String JMX_REMOTE_PORT_SYSTEM_PROPERTY = "com.sun.management.jmxremote.port";

    public static void main(final String arg[]) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP( Agent.class.getName(), "Starts a BUbiNG agent (note that you must enable JMX by means of the standard Java system properties).",
                new Parameter[] {
                        new FlaggedOption( "weight", JSAP.INTEGER_PARSER, "1", JSAP.NOT_REQUIRED, 'w', "weight", "The agent weight." ),
                        new FlaggedOption( "group", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'g', "group", "The JGroups group identifier (must be the same for all cooperating agents)." ),
                        new FlaggedOption( "jmxHost", JSAP.STRING_PARSER, InetAddress.getLocalHost().getHostAddress(), JSAP.REQUIRED, 'h', "jmx-host", "The IP address (possibly specified by a host name) that will be used to expose the JMX RMI connector to other agents." ),
                        new FlaggedOption( "rootDir", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'r', "root-dir", "The root directory." ),
                        new Switch( "new", 'n', "new", "Start a new crawl" ),
                        new FlaggedOption( "properties", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'P', "properties", "The properties used to configure the agent." ),
                        new UnflaggedOption( "name", JSAP.STRING_PARSER, JSAP.REQUIRED, "The agent name (an identifier that must be unique across the group)." )
                });

        JSAPResult jsapResult = jsap.parse( arg );
        if ( jsap.messagePrinted() ) System.exit( 1 );

        // JMX *must* be set up.
        final String portProperty = System.getProperty( JMX_REMOTE_PORT_SYSTEM_PROPERTY );
        if ( portProperty == null ) throw new IllegalArgumentException( "You must specify a JMX service port using the property " + JMX_REMOTE_PORT_SYSTEM_PROPERTY );

        final String name = jsapResult.getString( "name" );
        final int weight = jsapResult.getInt( "weight" );
        final String group = jsapResult.getString( "group" );
        final String host = jsapResult.getString( "jmxHost" );
        final int port = Integer.parseInt( portProperty );
        final String rootDir = jsapResult.getString( "rootDir" );
        System.out.println("RootDir "+rootDir);

        BaseConfiguration additional = new BaseConfiguration();
        additional.addProperty( "name", name );
        additional.addProperty( "group", group );
        additional.addProperty( "weight", Integer.toString( weight ) );
        additional.addProperty( "crawlIsNew", Boolean.valueOf( jsapResult.getBoolean( "new" ) ) );
        if ( jsapResult.userSpecified( "rootDir" ) ) additional.addProperty( "rootDir", rootDir );
        VisualIndexer.createInstance(rootDir);
        new Agent( host, port, new RuntimeConfiguration( new StartupConfiguration( jsapResult.getString( "properties" ), additional ) ) );
        System.exit( 0 ); // Kills remaining FetchingThread instances, if any.
    }
}
