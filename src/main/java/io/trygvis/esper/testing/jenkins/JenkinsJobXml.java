package io.trygvis.esper.testing.jenkins;

import java.net.URI;

import org.jdom2.Element;

import fj.F;
import fj.data.Option;
import io.trygvis.esper.testing.Util;

import static fj.data.Option.some;
import static io.trygvis.esper.testing.Util.child;
import static io.trygvis.esper.testing.Util.childText;

class JenkinsJobXml {
    enum JenkinsJobType {
        FREE_STYLE, MAVEN_MODULE_SET, MAVEN_MODULE, MATRIX
    }

    public final JenkinsJobType type;
    public final Option<String> description;
    public final Option<String> displayName;
    public final Option<String> name;
    public final URI url;
    public final Option<String> color;
    public final boolean buildable;
    public final Option<BuildXml> lastBuild;
    public final Option<BuildXml> lastCompletedBuild;
    public final Option<BuildXml> lastFailedBuild;
    public final Option<BuildXml> lastSuccessfulBuild;
    public final Option<BuildXml> lastUnsuccessfulBuild;

    protected JenkinsJobXml(JenkinsJobType type, Option<String> description, Option<String> displayName,
                            Option<String> name, URI url, Option<String> color, boolean buildable,
                            Option<BuildXml> lastBuild, Option<BuildXml> lastCompletedBuild,
                            Option<BuildXml> lastFailedBuild, Option<BuildXml> lastSuccessfulBuild,
                            Option<BuildXml> lastUnsuccessfulBuild) {
        this.type = type;
        this.description = description;
        this.displayName = displayName;
        this.name = name;
        this.url = url;
        this.color = color;
        this.buildable = buildable;
        this.lastBuild = lastBuild;
        this.lastCompletedBuild = lastCompletedBuild;
        this.lastFailedBuild = lastFailedBuild;
        this.lastSuccessfulBuild = lastSuccessfulBuild;
        this.lastUnsuccessfulBuild = lastUnsuccessfulBuild;
    }

    static class BuildXml {
        public final int number;
        public final URI url;
        public static F<Element, Option<BuildXml>> buildXml = new F<Element, Option<BuildXml>>() {
            public Option<BuildXml> f(Element element) {
                Option<Integer> number = childText(element, "number").bind(Util.parseInt);
                Option<URI> url = childText(element, "url").bind(Util.parseUri);

                if (number.isNone() || url.isNone()) {
                    return Option.none();
                }

                return some(new BuildXml(number.some(), url.some()));
            }
        };

        BuildXml(int number, URI url) {
            this.number = number;
            this.url = url;
        }
    }

    public static JenkinsJobXml parse(URI url, JenkinsJobType type, Element root) {
        return new JenkinsJobXml(type,
            childText(root, "description"),
            childText(root, "displayName"),
            childText(root, "name"),
            childText(root, "url").bind(Util.parseUri).orSome(url),
            childText(root, "color"),
            childText(root, "buildable").bind(Util.parseBoolean).orSome(false),
            child(root, "lastBuild").bind(BuildXml.buildXml),
            child(root, "lastCompletedBuild").bind(BuildXml.buildXml),
            child(root, "lastFailedBuild").bind(BuildXml.buildXml),
            child(root, "lastSuccessfulBuild").bind(BuildXml.buildXml),
            child(root, "lastUnsuccessfulBuild").bind(BuildXml.buildXml));
    }
}
