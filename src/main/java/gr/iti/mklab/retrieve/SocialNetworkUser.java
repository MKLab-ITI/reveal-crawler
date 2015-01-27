package gr.iti.mklab.retrieve;

import com.google.gdata.data.Link;
import com.google.gdata.data.media.mediarss.MediaThumbnail;
import com.google.gdata.data.youtube.UserProfileEntry;
import com.google.gdata.data.youtube.YtUserProfileStatistics;
import gr.iti.mklab.simmo.UserAccount;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by kandreadou on 1/26/15.
 */
@Entity("UserAccount")
public class SocialNetworkUser extends UserAccount {

    protected String pageUrl;

    protected String location;

    protected String description;

    public SocialNetworkUser() {
    }

    public SocialNetworkUser(UserProfileEntry user) {
        if (user == null)
            return;
        setSourceId("YouTube#" + user.getUsername());
        setStreamId("YouTube");

        //The name of the user
        name = (user.getFirstName() == null ? "" : user.getFirstName() + " ") + (user.getLastName() == null ? "" : user.getLastName());


        Link link = user.getLink("alternate", "text/html");
        if (link != null) {
            pageUrl = link.getHref();
        }
        location = user.getLocation();
        description = user.getAboutMe();
        MediaThumbnail thumnail = user.getThumbnail();
        setAvatarBig(thumnail.getUrl());
        YtUserProfileStatistics statistics = user.getStatistics();
        if (statistics != null) {
            setNumFollowers((int) statistics.getSubscriberCount());
        }
    }
}
