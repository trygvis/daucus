package io.trygvis.esper.testing;

import fj.*;
import fj.data.*;
import static fj.data.Option.*;
import org.jdom2.*;

import java.net.*;

public class Util {
    public static F<String, Option<Integer>> parseInt = new F<String, Option<Integer>>() {
        public Option<Integer> f(String s) {
            try {
                return some(Integer.parseInt(s));
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
}
