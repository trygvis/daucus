package io.trygvis.esper.testing.gitorious;

import fj.data.*;
import org.apache.abdera.*;
import org.apache.abdera.model.*;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.parser.*;
import org.dom4j.*;
import org.dom4j.io.*;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class GitoriousAtomFeedParser {
    public final Parser parser;
    public static final STAXEventReader xmlReader = new STAXEventReader();

    public GitoriousAtomFeedParser() {
        Abdera abdera = new Abdera();
        parser = abdera.getParser();
    }

    public List<GitoriousEvent> parseStream(InputStream stream, Option<Date> lastUpdate, String projectSlug, String name) {
        Document<Element> document = parser.parse(stream);

        Feed feed = (Feed) document.getRoot();

        List<GitoriousEvent> events = new ArrayList<>();

        for (Entry entry : feed.getEntries()) {
            Date published = entry.getPublished();

            // Check if it's old
            if (published == null || lastUpdate.isSome() && lastUpdate.some().after(published)) {
                continue;
            }

            GitoriousEvent event = parseEntry(projectSlug, name, entry);

            if (event == null) {
                continue;
            }

            events.add(event);
        }

        return events;
    }

    private static Pattern pPatternFixer = Pattern.compile("<p>$", Pattern.MULTILINE);
    private static Pattern branchPattern = Pattern.compile(".*/(.*)$");
    private static Pattern fromToPattern = Pattern.compile(".*/commit/([0-9a-f]*)/diffs/([0-9a-f]*)");

    private static GitoriousEvent parseEntry(String projectSlug, String name, Entry entry) {
        String entryId = entry.getId().toASCIIString();
        Date published = entry.getPublished();
        String title = entry.getTitle();

        // Validate element
        if (entryId == null || published == null || title == null) {
            return null;
        }

        String text = entry.getContent();

        text = pPatternFixer.matcher(text).replaceFirst("</p>");

        org.dom4j.Element content;
        String xml = "<p xmlns:gts='urn:gitorious'>" + text + "</p>";
        try {
            content = xmlReader.readDocument(new StringReader(xml)).getRootElement();

            List<org.dom4j.Element> elements = elements(content);
            List<Node> nodes = nodes(elements.get(0));

            String who = nodes.get(0).getText();

            String event = nodes.get(1).getText().trim();
            switch (event) {
                case "created repository":
                case "created branch":
                // This is similar "pushed", but doesn't contain any info on commit IDs or branches
                case "started development of":
                    return null;
                case "pushed":
                    org.dom4j.Element two = (org.dom4j.Element) nodes.get(2);
                    org.dom4j.Element six = (org.dom4j.Element) nodes.get(6);

                    Matcher branchMatcher = branchPattern.matcher(two.attributeValue("href"));
                    branchMatcher.matches();
                    String branch = branchMatcher.group(1);

                    String href = six.attributeValue("href");
                    Matcher matcher = fromToPattern.matcher(href);
                    matcher.matches();
                    String from = matcher.group(1);
                    String to = matcher.group(2);
                    int commitCount = Integer.parseInt(two.getText().replaceFirst("([0-9]*) commit[s]?", "\\1"));
                    return new GitoriousPush(projectSlug, name, entryId, published, title, text, who, from, to, branch, commitCount);
                default:
                    System.out.println("Unknown event: " + event);
                    return null;
            }
        } catch (Exception e) {
            System.out.println("Could not process: " + xml);
            return null;
        }
    }

    private static List<Node> nodes(org.dom4j.Element element) {
        List<Node> nodes = new ArrayList<>(element.nodeCount());

        @SuppressWarnings("unchecked") Iterator<Node> iterator = element.nodeIterator();
        while (iterator.hasNext()) {
            nodes.add(iterator.next());
        }
        return nodes;
    }

    private static List<org.dom4j.Element> elements(org.dom4j.Element content) {
        List<org.dom4j.Element> elements = new ArrayList<>();

        @SuppressWarnings("unchecked") Iterator<org.dom4j.Element> iterator = content.elementIterator();
        while (iterator.hasNext()) {
            elements.add(iterator.next());
        }
        return elements;
    }
}

abstract class GitoriousEvent {
    public final String projectSlug;
    public final String name;
    public final String entryId;
    public final Date published;
    public final String title;
    public final String content;
    public final String who;

    protected GitoriousEvent(String projectSlug, String name, String entryId, Date published, String title, String content, String who) {
        this.projectSlug = projectSlug;
        this.name = name;
        this.entryId = entryId;
        this.published = published;
        this.title = title;
        this.content = content;
        this.who = who;
    }
}

class GitoriousPush extends GitoriousEvent {
    public final String from;
    public final String to;
    public final String branch;
    public final int commitCount;

    GitoriousPush(String projectSlug, String name, String entryId, Date published, String title, String content, String who, String from, String to, String branch, int commitCount) {
        super(projectSlug, name, entryId, published, title, content, who);
        this.from = from;
        this.to = to;
        this.branch = branch;
        this.commitCount = commitCount;
    }
}
