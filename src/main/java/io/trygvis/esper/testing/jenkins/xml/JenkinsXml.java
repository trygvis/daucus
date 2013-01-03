package io.trygvis.esper.testing.jenkins.xml;

import java.util.List;

import fj.data.Option;

public class JenkinsXml {
    public final Option<String> nodeName;
    public final Option<String> nodeDescription;
    public final Option<String> description;
    public final List<JobXml> jobs;

    public JenkinsXml(Option<String> nodeName, Option<String> nodeDescription, Option<String> description, List<JobXml> jobs) {
        this.nodeName = nodeName;
        this.nodeDescription = nodeDescription;
        this.description = description;
        this.jobs = jobs;
    }

    public static class JobXml {
        public final String name;
        public final String url;
        public final String color;

        public JobXml(String name, String url, String color) {
            this.name = name;
            this.url = url;
            this.color = color;
        }
    }
}
