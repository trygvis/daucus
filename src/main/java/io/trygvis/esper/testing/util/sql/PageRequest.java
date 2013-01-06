package io.trygvis.esper.testing.util.sql;

import fj.data.*;

public class PageRequest {
    public final Option<Integer> startIndex;
    public final Option<Integer> count;
    public final List<String> orderBy;
    public static final PageRequest FIRST_PAGE = new PageRequest(Option.<Integer>none(), Option.<Integer>none(), List.<String>nil());

    public PageRequest(Option<Integer> startIndex, Option<Integer> count, List<String> orderBy) {
        this.startIndex = startIndex;
        this.count = count;
        this.orderBy = orderBy;
    }

    public String toString() {
        return "PageRequest{startIndex=" + startIndex + ", count=" + count + '}';
    }
}
