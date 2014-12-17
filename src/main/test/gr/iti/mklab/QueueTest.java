package gr.iti.mklab;

import gr.iti.mklab.queue.CrawlQueueController;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by kandreadou on 12/16/14.
 */
public class QueueTest {

    private CrawlQueueController controller;

    @Before
    public void setup() {
        controller = new CrawlQueueController();
    }

    @Test
    public void test(){
        stop();
    }

    public void start(){
        controller.submit("refactor", "refactor");
    }

    public void stop(){
        controller.cancel(9995);
    }
}
