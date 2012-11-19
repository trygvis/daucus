package io.trygvis.esper.testing;

import com.espertech.esper.client.*;

public class Main {
//    private static final String JDBC_URL = "jdbc:h2:mem:esper;DB_CLOSE_DELAY=-1";
    private static final String JDBC_URL = "jdbc:h2:tcp://127.0.0.1/esper;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws Exception {
        Config.loadFromDisk();
        Main main = new Main();
        main.work();
    }

    private void work() throws Exception {
        Configuration config = new Configuration();

        ConfigurationDBRef configurationDBRef = new ConfigurationDBRef();
        configurationDBRef.setDriverManagerConnection("org.h2.Driver", JDBC_URL, "", "");
        configurationDBRef.setConnectionAutoCommit(false);
        config.addDatabaseReference("db1", configurationDBRef);
        config.addEventTypeAutoName(getClass().getPackage().getName());
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);

//        String expression = "select avg(price) from OrderEvent.win:time(30 sec)";

        String expression = "select price, SUBSCRIBER from OrderEvent.win:time(30 sec)," +
            "sql:db1 ['select subscriber from subscription where itemName=${itemName}']";

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);

        MyListener listener = new MyListener();
        statement.addListener(listener);

        System.out.println("Inserting events");
        epService.getEPRuntime().sendEvent(new OrderEvent("shirt", 72));
        epService.getEPRuntime().sendEvent(new OrderEvent("shirt", 73));
        epService.getEPRuntime().sendEvent(new OrderEvent("shirt", 74));

        System.out.println("Sleeping");
        Thread.sleep(1000);
        System.out.println("Done..");
    }
}

class OrderEvent {
    private String itemName;
    private double price;

    public OrderEvent(String itemName, double price) {
        this.itemName = itemName;
        this.price = price;
    }

    public String getItemName() {
        return itemName;
    }

    public double getPrice() {
        return price;
    }
}

class MyListener implements UpdateListener {
    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        for (EventBean event : newEvents) {
            System.out.println("event.getEventType() = " + event.getEventType());
            System.out.println("event.getUnderlying() = " + event.getUnderlying());
            System.out.println("avg=" + event.get("price"));
        }
    }
}
