package gr.iti.mklab.retrieve;

import com.google.api.gbase.client.Thumbnail;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoStatistics;
import com.google.gdata.data.extensions.Rating;
import com.google.gdata.data.media.mediarss.MediaDescription;
import com.google.gdata.data.media.mediarss.MediaPlayer;
import com.google.gdata.data.media.mediarss.MediaThumbnail;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.YouTubeMediaContent;
import com.google.gdata.data.youtube.YouTubeMediaGroup;
import com.google.gdata.data.youtube.YtStatistics;
import gr.iti.mklab.simmo.UserAccount;
import gr.iti.mklab.simmo.items.Video;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;

import java.util.Date;
import java.util.List;

/**
 * Created by kandreadou on 1/16/15.
 */
@Entity("Video")
public class SocialNetworkVideo extends Video {

    protected String socialNetworkId;

    protected String networkName;

    protected long numLikes;

    protected long numViews;

    protected float rating;

    @Embedded
    public SocialNetworkUser user;

    public SocialNetworkVideo(){}

    public SocialNetworkVideo(com.google.api.services.youtube.model.Video v, Channel c) {
        socialNetworkId = "Youtube#"+v.getId();
        networkName = "Youtube";
        title = v.getSnippet().getTitle();
        description = v.getSnippet().getDescription();
        creationDate = new Date(v.getSnippet().getPublishedAt().getValue());
        crawlDate = new Date();
        VideoStatistics statistics = v.getStatistics();
        if(statistics!=null){
            numLikes = statistics.getFavoriteCount().longValue();
            numViews = statistics.getViewCount().longValue();
        }
        VideoContentDetails details = v.getContentDetails();
        if(details!=null){
            quality = details.getDefinition();
        }
        com.google.api.services.youtube.model.Thumbnail t = v.getSnippet().getThumbnails().getHigh();
        setThumbnail(t.getUrl());
        setWidth(t.getWidth().intValue());
        setHeight(t.getHeight().intValue());
        url =  "https://www.youtube.com/watch?v=" + v.getId();
        webPageUrl = url;

        user = new SocialNetworkUser(c);
    }

    public SocialNetworkVideo(VideoEntry v) {

        if (v == null || v.getId() == null)
            return;

        YouTubeMediaGroup mediaGroup = v.getMediaGroup();
        author = mediaGroup.getUploader();
        socialNetworkId = "Youtube#"+mediaGroup.getVideoId();
        networkName = "Youtube";
        //Timestamp of the creation of the video
        creationDate = new Date(mediaGroup.getUploaded().getValue());
        //Title of the video
        title = mediaGroup.getTitle().getPlainTextContent();
        //Description of the video
        MediaDescription desc = mediaGroup.getDescription();
        description = desc == null ? "" : desc.getPlainTextContent();

        //Popularity
        YtStatistics statistics = v.getStatistics();
        if (statistics != null) {
            numLikes = statistics.getFavoriteCount();
            numViews = statistics.getViewCount();
        }
        Rating rating = v.getRating();
        if(rating != null) {
            this.rating = rating.getAverage();
        }

        //Getting the video
        List<MediaThumbnail> thumbnails = mediaGroup.getThumbnails();
        MediaPlayer mediaPlayer = mediaGroup.getPlayer();

        setDuration(mediaGroup.getDuration());

        List<YouTubeMediaContent> mediaContent = mediaGroup.getYouTubeContents();

        String videoURL = null;
        for (YouTubeMediaContent content : mediaContent) {
            if (content.getType().equals("application/x-shockwave-flash")) {
                videoURL = content.getUrl();
                break;
            }
        }
        if (videoURL == null)
            videoURL = mediaPlayer.getUrl();
        url = videoURL;
        webPageUrl = mediaPlayer.getUrl();

        int size = 0;
        MediaThumbnail thumbnail = null;
        for (MediaThumbnail thumb : thumbnails) {
            int t_size = thumb.getWidth() * thumb.getHeight();
            if (size < t_size) {
                size = t_size;
                thumbnail = thumb;
            }
        }
        setThumbnail(thumbnail.getUrl());
        setHeight(thumbnail.getHeight());
        setWidth(thumbnail.getWidth());
    }
}

