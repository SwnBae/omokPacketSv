package com.keykycode.omok.adapter.memory.generator;

import com.keykycode.omok.application.port.out.PlayerIdGenerator;

public class SequencePlayerIdGenerator implements PlayerIdGenerator {

    private long last = 0;

    @Override
    public long next() {
        last++;
        return last;
    }
}
