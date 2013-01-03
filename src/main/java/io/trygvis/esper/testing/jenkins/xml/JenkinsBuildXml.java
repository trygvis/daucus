package io.trygvis.esper.testing.jenkins.xml;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fj.F;
import fj.data.Option;
import io.trygvis.esper.testing.Util;

import static fj.data.List.iterableList;
import static fj.data.Option.*;
import static io.trygvis.esper.testing.Util.childText;
import static io.trygvis.esper.testing.Util.parseInt;
import static io.trygvis.esper.testing.jenkins.xml.JenkinsBuildXml.ChangeSetItemXml.parseChangeSetItem;
import static io.trygvis.esper.testing.jenkins.xml.JenkinsBuildXml.RevisionXml.parseRevision;

public class JenkinsBuildXml {
    private static final Logger logger = LoggerFactory.getLogger(JenkinsBuildXml.class);

    public final URI url;

    public final int number;

    public final Option<String> result;

    public final int duration;

    public final long timestamp;

    public final Option<ChangeSetXml> changeSet;

    JenkinsBuildXml(URI url, int number, Option<String> result, int duration, long timestamp, Option<ChangeSetXml> changeSet) {
        this.url = url;
        this.number = number;
        this.result = result;
        this.duration = duration;
        this.timestamp = timestamp;
        this.changeSet = changeSet;
    }

    public static F<Document, Option<JenkinsBuildXml>> parse = new F<Document, Option<JenkinsBuildXml>>() {
        public Option<JenkinsBuildXml> f(Document document) {
            return parse(document);
        }
    };

    public static Option<JenkinsBuildXml> parse(Document doc) {
        Element root = doc.getRootElement();

        Option<URI> url = childText(root, "url").bind(Util.parseUri);
        Option<Integer> number = childText(root, "number").bind(Util.parseInt);
        Option<String> result = childText(root, "result");
        Option<Integer> duration = childText(root, "duration").bind(Util.parseInt);
        Option<Long> timestamp = childText(root, "timestamp").bind(Util.parseLong);

        if (url.isNone()) {
            logger.warn("Missing required field: <url>");
            return none();
        }
        if (number.isNone()) {
            logger.warn("Missing required field: <number>");
            return none();
        }
        if (duration.isNone()) {
            logger.warn("Missing required field: <duration>");
            return none();
        }
        if (timestamp.isNone()) {
            logger.warn("Missing required field: <timestamp>");
            return none();
        }

        Option<ChangeSetXml> changeSet = none();
        Element e = root.getChild("changeSet");
        if (e != null) {
            changeSet = ChangeSetXml.parse(e);
        }

        return some(new JenkinsBuildXml(url.some(), number.some(), result, duration.some(), timestamp.some(), changeSet));
    }

    public static class ChangeSetXml {
        public final List<ChangeSetItemXml> items;

        public final Option<RevisionXml> revision;

        public ChangeSetXml(List<ChangeSetItemXml> items, Option<RevisionXml> revision) {
            this.items = items;
            this.revision = revision;
        }

        public static Option<ChangeSetXml> parse(Element changeSet) {

            List<ChangeSetItemXml> items = new ArrayList<>(Option.somes(iterableList(changeSet.getChildren("item")).map(parseChangeSetItem)).toCollection());

            Option<RevisionXml> revision = Option.fromNull(changeSet.getChild("revision")).bind(parseRevision);

            return some(new ChangeSetXml(items, revision));
        }
    }

    public static class ChangeSetItemXml {
        public final String commitId;

        public final DateTime date;

        public final String msg;

        public final Option<AuthorXml> author;

        public ChangeSetItemXml(String commitId, DateTime date, String msg, Option<AuthorXml> author) {
            this.commitId = commitId;
            this.date = date;
            this.msg = msg;
            this.author = author;
        }

        private static final F<String, Option<DateTime>> parseDate = new F<String, Option<DateTime>>() {
            // This variant is used by git
            DateTimeFormatter parser = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z");
            // and this by subversion
            DateTimeFormatter parser2 = ISODateTimeFormat.dateTime();

            public Option<DateTime> f(String s) {
                try {
                    return some(parser.parseDateTime(s));
                } catch (IllegalArgumentException e) {
                    try {
                        return some(parser2.parseDateTime(s));
                    } catch (IllegalArgumentException e2) {
                        return none();
                    }
                }
            }
        };

        public static final F<Element, Option<ChangeSetItemXml>> parseChangeSetItem = new F<Element, Option<ChangeSetItemXml>>() {
            public Option<ChangeSetItemXml> f(Element item) {
                Option<String> commitId = childText(item, "commitId");
                Option<DateTime> date = childText(item, "date").bind(parseDate);
                Option<String> msg = childText(item, "msg");

                if (commitId.isNone() || date.isNone() || msg.isNone()) {
                    return none();
                }

                Option<AuthorXml> author = fromNull(item.getChild("author")).bind(AuthorXml.parseAuthorXml);

                return some(new ChangeSetItemXml(commitId.some(), date.some(), msg.some(), author));
            }
        };
    }

    public static class AuthorXml {
        public final String absoluteUrl;
        public final String fullName;

        public AuthorXml(String absoluteUrl, String fullName) {
            this.absoluteUrl = absoluteUrl;
            this.fullName = fullName;
        }

        public static final F<Element, Option<AuthorXml>> parseAuthorXml = new F<Element, Option<AuthorXml>>() {
            public Option<AuthorXml> f(Element element) {
                Option<String> absoluteUrl = childText(element, "absoluteUrl");
                Option<String> fullName = childText(element, "fullName");

                if(absoluteUrl.isNone() || fullName.isNone()) {
                    return none();
                }

                return some(new AuthorXml(absoluteUrl.some(), fullName.some()));
            }
        };
    }

    public static class RevisionXml {
        public final String module;

        public final int revision;

        public RevisionXml(String module, int revision) {
            this.module = module;
            this.revision = revision;
        }

        public static final F<Element, Option<RevisionXml>> parseRevision = new F<Element, Option<RevisionXml>>() {
            public Option<RevisionXml> f(Element e) {
                Option<String> module = childText(e, "module");
                Option<Integer> revision = childText(e, "revision").bind(parseInt);

                if (module.isNone() || revision.isNone()) {
                    return none();
                }

                return some(new RevisionXml(module.some(), revision.some()));
            }
        };
    }
}
