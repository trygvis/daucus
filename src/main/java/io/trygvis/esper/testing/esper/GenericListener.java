package io.trygvis.esper.testing.esper;

import com.espertech.esper.client.*;
import org.slf4j.*;

public class GenericListener implements UpdateListener {

    private final Logger logger;

    public GenericListener(Logger logger) {
        this.logger = logger;
    }

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        for (EventBean event : newEvents) {
            logger.info("new event");
            EventType eventType = event.getEventType();

            for (String name : eventType.getPropertyNames()) {
                System.out.println(name + " = " + event.get(name));
            }
        }
    }
}
