package io.milton.cloud.server.web.calc;

import io.milton.cloud.server.web.SpliffyResource;

interface Accumulator {

    void accumulate(SpliffyResource r, Object o);
}
