package io.trygvis.esper.testing.nexus;

import fj.*;
import fj.data.*;
import static fj.data.Option.*;
import org.jdom2.*;
import static org.jdom2.filter.Filters.*;
import org.joda.time.*;
import org.joda.time.format.*;

import java.net.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class NexusFeedParser {
    private static Namespace dc = Namespace.getNamespace("http://purl.org/dc/elements/1.1/");

    public static NexusFeed parseDocument(Document document) {
        List<Element> channels = document.getRootElement().getContent(element("channel"));

        List<NexusEvent> events = new ArrayList<>();

        if (channels.size() != 1) {
            return new NexusFeed(events);
        }

        Element channel = channels.get(0);

        for (Element item : channel.getContent(element("item"))) {
            Option<NexusEvent> e = parseEvent(item);

            if (e.isNone()) {
                continue;
            }

            events.add(e.some());
        }

        return new NexusFeed(events);
    }

    public static Option<NexusEvent> parseEvent(Element item) {
        String title = item.getChildText("title");

        Option<String> guid = Option.fromNull(item.getChildText("guid"));
        Option<String> creator = Option.fromNull(item.getChildText("creator", dc));
        Option<DateTime> date = Option.fromNull(item.getChildText("date", dc)).bind(new F<String, Option<DateTime>>() {
            @Override
            public Option<DateTime> f(String s) {
                try {
                    return some(ISODateTimeFormat.dateTimeNoMillis().parseDateTime(s));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    return Option.none();
                }
            }
        });

        String[] strings = title.split(":");

        String groupId;
        String artifactId;
        String version;
        Option<String> classifier;

        if (strings.length == 3) {
            groupId = strings[0];
            artifactId = strings[1];
            version = strings[2];
            classifier = Option.none();
        } else if (strings.length == 4) {
            groupId = strings[0];
            artifactId = strings[1];
            version = strings[2];
            classifier = some(strings[3]);
        } else {
            return null;
        }

        if (guid.isNone()) {
            System.out.println("Missing <guid>.");
            return null;
        }
        if (creator.isNone()) {
            System.out.println("Missing <dc:creator>.");
            return null;
        }
        if (date.isNone()) {
            System.out.println("Missing <dc:date>");
            return null;
        }

        Pattern regexp = Pattern.compile("(.*)-([0-9]{8}\\.[0-9]{6})-([0-9]*)$");

        Matcher matcher = regexp.matcher(version);

        if (matcher.matches()) {
            ArtifactId id = new ArtifactId(groupId, artifactId, matcher.group(1) + "-SNAPSHOT");
            Option<Integer> buildNumber = parseInt.f(matcher.group(3));

            if(buildNumber.isNone()) {
                System.out.println("Could not parse build number: " + matcher.group(3));
                return none();
            }

            return Option.<NexusEvent>some(new NewSnapshotEvent(guid.some(), id, classifier, creator.some(),
                    date.some(), matcher.group(2), buildNumber.some(), URI.create(item.getChildText("link"))));
        } else {
            ArtifactId id = new ArtifactId(groupId, artifactId, version);

            return Option.<NexusEvent>some(new NewReleaseEvent(guid.some(), id, classifier, creator.some(),
                    date.some(), URI.create(item.getChildText("link"))));
        }

//        System.out.println("Unknown event type.");
//
//        return none();
    }
}

class NexusFeed {
    List<NexusEvent> events = new ArrayList<>();

    NexusFeed(List<NexusEvent> events) {
        this.events = events;
    }
}

abstract class NexusEvent {
    public final String guid;
    public final ArtifactId artifactId;
    public final Option<String> classifier;
    public final String creator;
    public final DateTime date;

    NexusEvent(String guid, ArtifactId artifactId, Option<String> classifier, String creator, DateTime date) {
        this.guid = guid;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.creator = creator;
        this.date = date;
    }
}

class NewSnapshotEvent extends NexusEvent {
    public final String snapshotTimestamp;
    public final int buildNumber;
    public final URI url;

    NewSnapshotEvent(String guid, ArtifactId artifactId, Option<String> classifier, String creator, DateTime date, String snapshotTimestamp, int buildNumber, URI url) {
        super(guid, artifactId, classifier, creator, date);
        this.snapshotTimestamp = snapshotTimestamp;
        this.buildNumber = buildNumber;
        this.url = url;
    }
}

class NewReleaseEvent extends NexusEvent {
    public final URI url;

    NewReleaseEvent(String guid, ArtifactId artifactId, Option<String> classifier, String creator, DateTime date, URI url) {
        super(guid, artifactId, classifier, creator, date);
        this.url = url;
    }
}
