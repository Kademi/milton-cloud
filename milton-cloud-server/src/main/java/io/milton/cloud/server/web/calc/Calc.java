package io.milton.cloud.server.web.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffyResource;
import io.milton.cloud.server.web.templating.Formatter;

/**
 *
 */
public class Calc {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Calc.class);
    private final List<SpliffyResource> list;
    private final Formatter formatter;

    public Calc(List<SpliffyResource> list, Formatter formatter) {
        this.list = list;
        this.formatter = formatter;
    }

    public Object eval(String mvelExpr, Object r) {
        HashMap map = new HashMap();
        Object o = org.mvel.MVEL.eval(mvelExpr, r, map);
        return o;
    }

    public BigDecimal sum(String mvelExpr) {
        return sum(mvelExpr, 0);
    }

    public ResourceList filter(String mvelExpr) {
        //log.debug( "filter");
        ListFilter filter = new ListFilter();
        accumulate(filter, mvelExpr);
        return filter.dest;
    }

    public BigDecimal sum(String mvelExpr, int decimals) {
//        log.debug("sum: " + mvelExpr);
        Sumor summer = new Sumor(decimals, formatter);
        accumulate(summer, mvelExpr);
        return summer.value;
    }

    void accumulate(Accumulator a, String mvelExpr) {
        for (SpliffyResource r : list) {
            Object o = eval(mvelExpr, r);
            a.accumulate(r, o);
        }
    }
}
