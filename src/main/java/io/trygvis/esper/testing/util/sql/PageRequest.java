package io.trygvis.esper.testing.util.sql;

import fj.data.*;
import static fj.data.Option.fromNull;
import io.trygvis.esper.testing.*;

import javax.servlet.http.*;

public class PageRequest {
    public final Option<Integer> startIndex;
    public final Option<Integer> count;

    public PageRequest(Option<Integer> startIndex, Option<Integer> count) {
        this.startIndex = startIndex;
        this.count = count;
    }

    public static PageRequest pageReq(HttpServletRequest req) {
        return new PageRequest(
            fromNull(req.getParameter("startIndex")).bind(Util.parseInt),
            fromNull(req.getParameter("count")).bind(Util.parseInt));
    }
}
