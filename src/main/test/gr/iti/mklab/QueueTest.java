package gr.iti.mklab;

import gr.iti.mklab.queue.CrawlQueueController;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by kandreadou on 12/16/14.
 */
public class QueueTest {

    public static void main(String[] args) throws Exception {
        CrawlQueueController controller = new CrawlQueueController();
        controller.submit(false, "crawltest1", "crawltest1", "malaysia", "flight", "crash", "disaster", "missing");
        //controller.submit("crawltest2", "crawltest2");
        //Thread.sleep(50000);
        //controller.submit("crawltest3", "crawltest3");
        //controller.submit("crawltest4", "crawltest4");
        //controller.cancel(9995);
    }
}
