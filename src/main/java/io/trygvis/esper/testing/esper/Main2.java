package io.trygvis.esper.testing.esper;

import com.espertech.esper.client.*;
import io.trygvis.esper.testing.*;

import javax.sql.*;
import java.sql.*;
import java.util.*;

public class Main2 {
    private static Config config;

    @SuppressWarnings("UnusedDeclaration")
    public static DataSource createDataSource(Properties p) throws SQLException {
        return config.createBoneCp();
    }

    public static void main(String[] args) throws Exception {
        config = Config.loadFromDisk();

        Configuration c = new Configuration();

        ConfigurationDBRef configurationDBRef = new ConfigurationDBRef();
        configurationDBRef.setDataSourceFactory(new Properties(), Main2.class.getName());
        configurationDBRef.setConnectionAutoCommit(true);
        c.addDatabaseReference("db1", configurationDBRef);

        c.addVariable("VarLastTimestamp", Long.class, Long.valueOf(0));

//        c.addEventTypeAutoName(getClass().getPackage().getName());

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(c);

//        String expression = "select avg(price) from OrderEvent.win:time(30 sec)";

        EPAdministrator administrator = epService.getEPAdministrator();

        EPStatement statement = administrator.createEPL("insert into JenkinsBuild " +
                "select uuid, job, result from pattern [every timer:interval(1 sec)], " +
                "sql:db1 ['SELECT uuid, job, result FROM jenkins_build WHERE extract(epoch from created_date) > ${VarLastTimestamp}']");

        JenkinsBuildListener listener = new JenkinsBuildListener();
        statement.addListener(listener);

        administrator.createEPL("on JenkinsBuild set VarLastTimestamp = current_timestamp()");

        administrator.createEPL("every build=JenkinsBuild(result='FAILURE') -> ((JenkinsBuild(job=build.job, result='FAILURE') and not Sample(sensor=sample.sensor, temp <= 50)) -> (Sample(sensor=sample.sensor, temp > 50) and not Sample(sensor=sample.sensor, temp <= 50))) where timer:within(90 seconds))\n");

        while(true) {
            Thread.sleep(10000);
        }
    }
}

class JenkinsBuildListener implements UpdateListener {
    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        for (EventBean event : newEvents) {
            System.out.println("uuid=" + event.get("uuid"));
        }
    }
}

/*
every build=JenkinsBuild(result='FAILURE') ->
 (
  (
   Sample(sensor=sample.sensor, temp > 50) and not
   Sample(sensor=sample.sensor, temp <= 50)
  ) ->
  (
   Sample(sensor=sample.sensor, temp > 50) and not
   Sample(sensor=sample.sensor, temp <= 50)
  )
 )
where timer:within(90 seconds))

*/
