package io.milton.cloud.server.web.calc;

import io.milton.cloud.server.web.CommonResource;

interface Accumulator {

    void accumulate(CommonResource r, Object o);
}
