package io.trygvis.esper.testing.util;

import fj.data.*;
import org.h2.util.*;
import org.jdom2.*;
import org.jdom2.input.*;
import org.slf4j.*;

import javax.xml.stream.*;
import java.io.*;

import static fj.data.Option.*;
import static javax.xml.stream.XMLStreamConstants.*;

public class XmlParser {
    private static final Logger logger = LoggerFactory.getLogger(XmlParser.class);

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
                logger.debug("------------------------------------------------");
                logger.debug(new String(bytes, "utf-8"));
                logger.debug("------------------------------------------------");
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
