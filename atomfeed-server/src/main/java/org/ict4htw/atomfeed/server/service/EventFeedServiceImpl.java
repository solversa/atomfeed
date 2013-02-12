package org.ict4htw.atomfeed.server.service;

import com.sun.syndication.feed.atom.*;
import org.ict4htw.atomfeed.server.domain.EventFeed;
import org.ict4htw.atomfeed.server.domain.EventRecord;
import org.ict4htw.atomfeed.server.domain.EventRecordComparator;
import org.ict4htw.atomfeed.server.domain.FeedBuilder;
import org.ict4htw.atomfeed.server.service.feedgenerator.FeedGenerator;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.*;

public class EventFeedServiceImpl implements EventFeedService {

    private static final String ATOM_MEDIA_TYPE = "application/atom+xml";
    private static final String LINK_TYPE_SELF = "self";
    private static final String LINK_TYPE_VIA = "via";
    private static final String ATOMFEED_MEDIA_TYPE = "application/vnd.atomfeed+xml";

	private FeedGenerator feedGenerator;

    public EventFeedServiceImpl(FeedGenerator generator) {
        this.feedGenerator = generator;
    }


    /* (non-Javadoc)
	 * @see org.ict4htw.atomfeed.server.service.EventFeedServiceInterface#getRecentFeed(java.net.URI)
	 */
    @Override
	public Feed getRecentFeed(URI requestUri) {
    	EventFeed recentFeed = feedGenerator.getRecentFeed();
        return new FeedBuilder()
                .type("atom_1.0")
                        // Presence of feedgenerator ID not necessary. We might choose to remove it
                .id("urn:uuid:" + UUID.randomUUID().toString())
                .title("TITLE") // TODO: This should be dictated by the application using it.
                .generator(getGenerator())
//                .authors() // TODO : Use Person class or rome and link to OpenMRS URI for user
                .entries(getEntries(recentFeed.getEvents(), requestUri))
                .updated(newestEventDate(recentFeed.getEvents()))
                .link(getLink(requestUri.toString(), LINK_TYPE_SELF, ATOM_MEDIA_TYPE))
                .link(getLink(generateCanonicalUri(requestUri, recentFeed.getId()), LINK_TYPE_VIA, ATOM_MEDIA_TYPE))
                .links(generatePagingLinks(requestUri, recentFeed))
                .build();
    }
    
    private String generateCanonicalUri(URI requestUri, Integer feedId) {
        return getServiceUri(requestUri) + "/" + feedId;
    }
    
    private List<Link> generatePagingLinks(URI requestUri, EventFeed feed) {
        ArrayList<Link> links = new ArrayList<Link>();
        int feedCount = feedGenerator.getRecentFeed().getId();

        if (feed.getId() < feedCount) {
            Link next = new Link();
            next.setRel("next-archive");
            next.setType(ATOM_MEDIA_TYPE);
            next.setHref(generateCanonicalUri(requestUri, feed.getId()+1));
            links.add(next);
        }

        if (feed.getId() > 1) {
            Link prev = new Link();
            prev.setRel("prev-archive");
            prev.setType(ATOM_MEDIA_TYPE);
            prev.setHref(generateCanonicalUri(requestUri, feed.getId()-1));
            links.add(prev);
        }
        return links;
    }
    

    /* (non-Javadoc)
	 * @see org.ict4htw.atomfeed.server.service.EventFeedServiceInterface#getEventFeed(java.net.URI, java.lang.Integer)
	 */
    @Override
	public Feed getEventFeed(URI requestUri, Integer feedId) {
        EventFeed feedForId = feedGenerator.getFeedForId(feedId);
        return new FeedBuilder()
                .type("atom_1.0")
                .id("urn:uuid:" + UUID.randomUUID().toString())
                .title("TITLE")
                .generator(getGenerator())
                .entries(getEntries(feedForId.getEvents(), requestUri))
                .updated(newestEventDate(feedForId.getEvents()))
                .link(getLink(requestUri.toString(), LINK_TYPE_SELF, ATOM_MEDIA_TYPE))
                .links(generatePagingLinks(requestUri,feedForId))
                .build();

    }

    private Generator getGenerator() {
        Generator generator = new Generator();
        generator.setUrl("http://uri");
        generator.setValue("FEED_SERVICE");
        return generator;
    }

    private Date newestEventDate(List<EventRecord> eventRecordList) {
        if(eventRecordList.isEmpty()){
            return new DateTime().toDateMidnight().toDate();
        }
        return Collections.max(eventRecordList, new EventRecordComparator()).getTimeStamp();
    }

    private Link getLink(String href, String rel, String type) {
        Link link = new Link();

        link.setHref(href);
        link.setRel(rel);
        link.setType(type);

        return link;
    }

    private String getServiceUri(URI requestUri) {
        String scheme = requestUri.getScheme();
        String hostname = requestUri.getHost();
        int port = requestUri.getPort();
        String path = requestUri.getPath().substring(0,
                requestUri.getPath().lastIndexOf("/"));
        if (port != 80 && port != -1) {
            return scheme + "://" + hostname + ":" + port + path;
        } else {
            return scheme + "://" + hostname + path;
        }
    }        

    private List<Entry> getEntries(List<EventRecord> eventRecordList, URI requestUri) {
        List<Entry> entryList = new ArrayList<Entry>();

        for (EventRecord eventRecord : eventRecordList) {
            final Entry entry = new Entry();
            entry.setId(eventRecord.getTagUri());
            entry.setTitle(eventRecord.getTitle());
            entry.setUpdated(eventRecord.getTimeStamp());
            entry.setAlternateLinks(generateLinks(eventRecord, requestUri));
            entry.setContents(generateContents(eventRecord));
            entryList.add(entry);
        }

        return entryList;
    }

    private List<Link> generateLinks(EventRecord eventRecord, URI requestUri) {
        Link self = new Link();
        self.setHref(getServiceUri(requestUri) + "/entries/" + eventRecord.getId());
        self.setRel("self");

        Link related = new Link();
        related.setHref(eventRecord.getUri());
        related.setRel("related");

        return Arrays.asList(self, related);
    }

    private List<Content> generateContents(EventRecord eventRecord) {
        Content content = new Content();
        content.setType(ATOMFEED_MEDIA_TYPE);
        content.setValue(eventRecord.getContents());
        return Arrays.asList(content);
    }
}