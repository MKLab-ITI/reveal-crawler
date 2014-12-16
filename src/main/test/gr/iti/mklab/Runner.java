package gr.iti.mklab;

import org.apache.commons.lang.ArrayUtils;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Created by kandreadou on 12/15/14.
 */
public class Runner {

    public static void main(String[] args) throws IOException, InterruptedException {
        //String[] command = {"/bin/bash", "start.sh"};

        try {
            String cmdStr = "java -server -Xss256K -Xms8G -Xmx8G -XX:+UseNUMA -XX:+UseConcMarkSweepGC " +
                    "-XX:+UseTLAB -XX:+ResizeTLAB -XX:NewRatio=4 -XX:MaxTenuringThreshold=15 -XX:+CMSParallelRemarkEnabled " +
                    "-verbose:gc -Xloggc:gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCApplicationStoppedTime " +
                    "-Djava.rmi.server.hostname=127.0.0.1 " +
                    "-Djava.net.preferIPv4Stack=true " +
                    "-Djgroups.bind_addr=127.0.0.1 " +
                    "-Dlogback.configurationFile=bubing-logback.xml " +
                    "-Dcom.sun.management.jmxremote.port=9993 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false " +
                    "-cp target/reveal-crawler-1.0-SNAPSHOT-jar-with-dependencies.jar gr.iti.mklab.bubing.ItiAgent " +
                    "-h 127.0.0.1 -r rootCrawl -P reveal.properties -g eu agent -n 2>err >out";
            String[] command = cmdStr.split("\\s+");
            System.out.println(command.length);
            System.out.println(ArrayUtils.toString(cmdStr));
            ProcessBuilder p = new ProcessBuilder(command);
            Process p2 = p.start();
            //invokeStop();
        }catch(Exception e){
            System.out.println(e);
        }
    }

    public static void enqueue(int portNumber, String crawlDir, String collectionName){

    }

    public static void invokeStop(int portNumber) {
        try {

            JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi://localhost/jndi/rmi://localhost:"+portNumber+"/jmxrmi");
            JMXConnector cc = JMXConnectorFactory.connect(jmxServiceURL);
            MBeanServerConnection mbsc = cc.getMBeanServerConnection();
            //This information is available in jconsole
            ObjectName  serviceConfigName = new ObjectName("it.unimi.di.law.bubing:type=Agent,name=agent");
            //  Invoke stop operation
            mbsc.invoke(serviceConfigName, "stop", null, null);
            //  Close JMX connector
            cc.close();
        } catch (Exception e) {
            System.out.println("Exception occurred: " + e.toString());
            e.printStackTrace();
        }
    }
}
