package io.trygvis.esper.testing;

import org.h2.tools.*;

import java.sql.*;

public class DbMain {
    private static final String JDBC_URL = "jdbc:h2:mem:esper;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws Exception {
        Server server = Server.createTcpServer(args).start();

        System.out.println("server.getURL() = " + server.getURL());

        Connection connection = DriverManager.getConnection(JDBC_URL, "", "");
        connection.setAutoCommit(false);
        Statement s = connection.createStatement();
        s.execute("create table subscription(" +
            "itemName varchar(100) not null," +
            "subscriber varchar(100) not null" +
            ");");

        s.execute("insert into subscription values('shirt', 'sub a');");
        s.execute("insert into subscription values('shirt', 'sub b');");
        s.execute("insert into subscription values('pants', 'sub b');");
        s.execute("insert into subscription values('pants', 'sub c');");
        connection.commit();

        while(true) {
            Thread.sleep(1000);
        }
    }
}
