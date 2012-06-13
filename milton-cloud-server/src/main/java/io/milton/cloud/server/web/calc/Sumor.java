package io.milton.cloud.server.web.calc;

import java.math.BigDecimal;
import io.milton.cloud.server.web.SpliffyResource;
import io.milton.cloud.server.web.templating.Formatter;

class Sumor implements Accumulator {

    private final int decimals;
    private final Formatter formatter;

    public Sumor(int decimals, Formatter formatter) {
        super();
        this.decimals = decimals;
        this.formatter = formatter;
    }
    BigDecimal value = new BigDecimal(0);

    @Override
    public void accumulate(SpliffyResource r, Object o) {
        BigDecimal bd = formatter.toBigDecimal(o, decimals);
        if (bd != null) {
            value = value.add(bd);
        }
    }
}
