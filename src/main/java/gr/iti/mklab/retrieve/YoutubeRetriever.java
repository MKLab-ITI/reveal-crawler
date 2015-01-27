package gr.iti.mklab.retrieve;

import com.google.gdata.client.youtube.YouTubeQuery;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.youtube.UserProfileEntry;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.VideoFeed;
import com.google.gdata.util.ServiceException;
import gr.iti.mklab.simmo.UserAccount;
import gr.iti.mklab.simmo.items.Video;
import gr.iti.mklab.simmo.morphia.MediaDAO;
import gr.iti.mklab.simmo.morphia.MorphiaManager;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.mongodb.morphia.dao.BasicDAO;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by kandreadou on 1/16/15.
 */
public class YoutubeRetriever {
    private final String activityFeedVideoUrlPrefix = "http://gdata.youtube.com/feeds/api/videos";
    private final String activityFeedUserUrlPrefix = "http://gdata.youtube.com/feeds/api/users/";
    private Logger logger = Logger.getLogger(YoutubeRetriever.class);

    private YouTubeService service;
    // This is an API restriction
    private final static int RESULTS_THRESHOLD = 500;
    private final static int REQUEST_THRESHOLD = 50;
    private final static long MAX_RUNNING_TIME = 120000; //2 MINUTES

    private final static String APP_NAME = "reveal-2015";
    private final static String DEV_ID = "AIzaSyA_DFJJ63kioLqZ09fH2kvIlqeNMrPvATU";

    private MediaDAO<Video> videoDAO;
    private BasicDAO<UserAccount,ObjectId> userDAO;

    public static void main(String[] args) throws Exception {
        MorphiaManager.setup("127.0.0.1");
        YoutubeRetriever r = new YoutubeRetriever("youtube");
        Set<String> set = new HashSet<String>();
        set.add("Ukraine");
        set.add("merkel");
        set.add("intervention");
        r.retrieve(set);
        MorphiaManager.tearDown();
    }

    public YoutubeRetriever(String collectionName) {

        this.service = new YouTubeService(APP_NAME, DEV_ID);
        videoDAO = new MediaDAO<Video>(Video.class, collectionName);
        userDAO = new BasicDAO<UserAccount, ObjectId>(UserAccount.class, MorphiaManager.getMongoClient(), MorphiaManager.getMorphia(), MorphiaManager.getDB(collectionName).getName());
    }

    public void retrieve(Set<String> keywords){
        List<Video> videos = retrieveKeywordsFeeds(keywords);
        for(Video v:videos){

            videoDAO.save(v);
            //UserAccount user = retrieveUser(v.getAuthor());
            //userDAO.save(user);
        }
    }

    private SocialNetworkUser retrieveUser(String uid) {

        URL profileUrl;
        try

        {
            profileUrl = new URL(activityFeedUserUrlPrefix + uid);
            UserProfileEntry userProfile = service.getEntry(profileUrl, UserProfileEntry.class);
            SocialNetworkUser user = new SocialNetworkUser(userProfile);
            return user;
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (ServiceException e)
        {
            logger.error(e.getMessage());
        }

        return null;
    }

    private List<Video> retrieveKeywordsFeeds(Set<String> keywords) {

        List<Video> items = new ArrayList<Video>();

        int startIndex = 1;
        int maxResults = 25;
        int currResults = 0;
        int numberOfRequests = 0;

        long currRunningTime = System.currentTimeMillis();

        if (keywords == null) {
            logger.error("#YouTube : No keywords feed");
            return items;
        }

        String tags = "";

        for (String key : keywords) {
            String[] words = key.split(" ");
            for (String word : words) {
                if (!tags.contains(word) && word.length() > 1)
                    tags += word.toLowerCase() + " ";
            }
        }

        //one call - 25 results
        if (tags.equals(""))
            return items;

        YouTubeQuery query;
        try {
            query = new YouTubeQuery(new URL(activityFeedVideoUrlPrefix));
        } catch (MalformedURLException e1) {

            return items;
        }

        query.setOrderBy(YouTubeQuery.OrderBy.PUBLISHED);
        query.setFullTextQuery(tags);
        query.setSafeSearch(YouTubeQuery.SafeSearch.NONE);
        query.setMaxResults(maxResults);

        VideoFeed videoFeed;
        while (true) {
            try {
                query.setStartIndex(startIndex);
                videoFeed = service.query(query, VideoFeed.class);

                numberOfRequests++;

                currResults = videoFeed.getEntries().size();
                startIndex += currResults;

                for (VideoEntry video : videoFeed.getEntries()) {
                    SocialNetworkVideo videoItem = new SocialNetworkVideo(video);
                    videoItem.setCrawlDate(new Date());
                    SocialNetworkUser u = retrieveUser(videoItem.getAuthor());
                    videoItem.user = u;
                    items.add(videoItem);

                    if (items.size() > RESULTS_THRESHOLD || numberOfRequests >= REQUEST_THRESHOLD || (System.currentTimeMillis() - currRunningTime) > MAX_RUNNING_TIME) {
                        return items;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                logger.error("YouTube Retriever error during retrieval of " + tags);
                logger.error("Exception: " + e.getMessage());
                return items;
            }
        }

    }
}
