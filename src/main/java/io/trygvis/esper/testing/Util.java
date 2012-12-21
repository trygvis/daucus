package io.trygvis.esper.testing;

import fj.*;
import fj.data.*;
import static fj.data.Option.*;

import io.trygvis.esper.testing.util.sql.*;
import org.jdom2.*;
import org.joda.time.*;

import java.net.*;
import java.sql.*;
import java.sql.Array;
import java.util.*;
import java.util.List;

public class Util {
    public static final F<Timestamp, java.util.Date> timestampToDate = new F<Timestamp, java.util.Date>() {
        public java.util.Date f(Timestamp timestamp) {
            return new java.util.Date(timestamp.getTime());
        }
    };

    public static final F<Timestamp, LocalDateTime> timestampToLocalDateTime = new F<Timestamp, LocalDateTime>() {
        public LocalDateTime f(Timestamp timestamp) {
            return new LocalDateTime(timestamp.getTime());
        }
    };

    public static final F<java.util.Date, Timestamp> dateToTimestamp = new F<java.util.Date, Timestamp>() {
        public Timestamp f(java.util.Date date) {
            return new Timestamp(date.getTime());
        }
    };

    public static F<String, Option<Integer>> parseInt = new F<String, Option<Integer>>() {
        public Option<Integer> f(String s) {
            try {
                return some(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return none();
            }
        }
    };

    public static F<String, Option<Long>> parseLong = new F<String, Option<Long>>() {
        public Option<Long> f(String s) {
            try {
                return some(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return none();
            }
        }
    };

    public static F<String, Option<URI>> parseUri = new F<String, Option<URI>>() {
        public Option<URI> f(String s) {
            try {
                return some(URI.create(s));
            } catch (Throwable e) {
                return none();
            }
        }
    };

    public static F<String, Option<Boolean>> parseBoolean = new F<String, Option<Boolean>>() {
        public Option<Boolean> f(String s) {
            try {
                return some(Boolean.parseBoolean(s));
            } catch (Throwable e) {
                return none();
            }
        }
    };

    public static Option<String> childText(Element e, String childName) {
        return fromNull(e.getChildText(childName));
    }

    public static Option<Element> child(Element e, String childName) {
        return fromNull(e.getChild(childName));
    }

    public static UUID[] toUuidArray(ResultSet rs, int index) throws SQLException {
        Array array = rs.getArray(index);
        if(array == null) {
            return new UUID[0];
        }
        String[] strings = (String[]) array.getArray();
        UUID[] uuids = new UUID[strings.length];
        for (int i = 0; i < strings.length; i++) {
            uuids[i] = UUID.fromString(strings[i]);
        }
        return uuids;
    }

    public static <A> List<A> toList(PreparedStatement s, SqlF<ResultSet, A> f) throws SQLException {
        List<A> list = new ArrayList<>();
        ResultSet rs = s.executeQuery();
        while(rs.next()) {
            list.add(f.apply(rs));
        }
        return list;
    }
}
