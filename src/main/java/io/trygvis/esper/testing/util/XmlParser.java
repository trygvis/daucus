package io.trygvis.esper.testing.util;

import fj.data.*;
import static fj.data.Option.*;
import static javax.xml.stream.XMLStreamConstants.*;
import org.h2.util.*;
import org.jdom2.*;
import org.jdom2.input.*;

import java.io.*;
import javax.xml.stream.*;

public class XmlParser {
    public static boolean debugXml;

    private final XMLInputFactory xmlInputFactory;

    private final StAXStreamBuilder streamBuilder = new StAXStreamBuilder();

    public XmlParser() {
        xmlInputFactory = XMLInputFactory.newFactory();
    }

    public Option<Document> parseDocument(InputStream stream) {

        try {
            if (debugXml) {
                // TODO: Pretty print
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                IOUtils.copy(stream, buffer);
                byte[] bytes = buffer.toByteArray();
                System.out.println("------------------------------------------------");
                System.out.write(bytes);
                System.out.println();
                System.out.println("------------------------------------------------");
                stream = new ByteArrayInputStream(bytes);
            }

            // https://github.com/hunterhacker/jdom/issues/101
            XMLStreamReader readerX = xmlInputFactory.createXMLStreamReader(stream);

            XMLStreamReader reader = xmlInputFactory.createFilteredReader(readerX, new StreamFilter() {

                boolean seenStartDocument;

                @Override
                public boolean accept(XMLStreamReader reader) {
                    if(reader.getEventType() == SPACE && !seenStartDocument) {
                        return false;
                    }

                    if(reader.getEventType() == START_DOCUMENT) {
                        seenStartDocument = false;
                    }

                    return true;
                }
            });

            return some(streamBuilder.build(reader));
        } catch (Exception e) {
            e.printStackTrace();
            return none();
        }
    }
}
