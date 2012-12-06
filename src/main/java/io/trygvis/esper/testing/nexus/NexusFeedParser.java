package io.trygvis.esper.testing.nexus;

import fj.*;
import fj.data.*;
import static fj.data.Option.*;
import static java.util.regex.Pattern.compile;
import org.jdom2.*;
import static org.jdom2.filter.Filters.*;
import org.joda.time.*;
import org.joda.time.format.*;

import java.net.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class NexusFeedParser {
    private static final Namespace dc = Namespace.getNamespace("http://purl.org/dc/elements/1.1/");

    private static final Pattern snapshotPattern =
            compile("(.*)-([0-9]{4})([0-9]{2})([0-9]{2})\\.([0-9]{2})([0-9]{2})([0-9]{2})-([0-9]*)$");

    public static NexusFeed parseDocument(Document document) {
        List<Element> channels = document.getRootElement().getContent(element("channel"));

        List<HasNexusEvent> events = new ArrayList<>();

        if (channels.size() != 1) {
            return new NexusFeed(events);
        }

        Element channel = channels.get(0);

        for (Element item : channel.getContent(element("item"))) {
            Option<HasNexusEvent> e = parseEvent(item);

            if (e.isNone()) {
                continue;
            }

            events.add(e.some());
        }

        return new NexusFeed(events);
    }

    public static Option<HasNexusEvent> parseEvent(Element item) {
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

        Matcher matcher = snapshotPattern.matcher(version);

        if (matcher.matches()) {
            ArtifactId id = new ArtifactId(groupId, artifactId, matcher.group(1) + "-SNAPSHOT");
            Option<Integer> buildNumber = parseInt.f(matcher.group(8));

            if(buildNumber.isNone()) {
                System.out.println("Could not parse build number: " + matcher.group(3));
                return none();
            }

            NexusEvent event = new NexusEvent(guid.some(), id, classifier, creator.some(), date.some());

            int year = Integer.parseInt(matcher.group(2));
            int month = Integer.parseInt(matcher.group(3));
            int day = Integer.parseInt(matcher.group(4));
            int hour = Integer.parseInt(matcher.group(5));
            int minute = Integer.parseInt(matcher.group(6));
            int second = Integer.parseInt(matcher.group(7));

            return Option.<HasNexusEvent>some(new NewSnapshotEvent(event, new DateTime(year, month, day, hour, minute, second, 0), buildNumber.some(),
                    URI.create(item.getChildText("link"))));
        } else {
            ArtifactId id = new ArtifactId(groupId, artifactId, version);

            NexusEvent event = new NexusEvent(guid.some(), id, classifier, creator.some(), date.some());

            return Option.<HasNexusEvent>some(new NewReleaseEvent(event, URI.create(item.getChildText("link"))));
        }
    }
}

class NexusFeed {
    List<HasNexusEvent> events = new ArrayList<>();

    NexusFeed(List<HasNexusEvent> events) {
        this.events = events;
    }
}

final class NexusEvent {
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

abstract class HasNexusEvent {
    public final NexusEvent event;

    protected HasNexusEvent(NexusEvent event) {
        this.event = event;
    }
}

class NewSnapshotEvent extends HasNexusEvent {
    public final DateTime snapshotTimestamp;
    public final int buildNumber;
    public final URI url;

    NewSnapshotEvent(NexusEvent event, DateTime snapshotTimestamp, int buildNumber, URI url) {
        super(event);
        this.snapshotTimestamp = snapshotTimestamp;
        this.buildNumber = buildNumber;
        this.url = url;
    }
}

class NewReleaseEvent extends HasNexusEvent {
    public final URI url;

    NewReleaseEvent(NexusEvent event, URI url) {
        super(event);
        this.url = url;
    }
}
