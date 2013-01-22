package io.trygvis.esper.testing.util.sql;

import fj.data.*;

import java.util.*;
import java.util.List;

import static fj.data.Option.*;

public class PageRequest {
    public final Option<Integer> startIndex;
    public final Option<Integer> count;
    public final List<String> orderBy;
    public static final PageRequest FIRST_PAGE = new PageRequest(Option.<Integer>none(), Option.<Integer>none(), Collections.<String>emptyList());

    public PageRequest(Option<Integer> startIndex, Option<Integer> count, List<String> orderBy) {
        this.startIndex = startIndex;
        this.count = count;
        this.orderBy = orderBy;
    }

    public String toString() {
        return "PageRequest{startIndex=" + startIndex + ", count=" + count + '}';
    }

    public static PageRequest one(String... orderBy) {
        return new PageRequest(some(0), some(1), Arrays.asList(orderBy));
    }
}
