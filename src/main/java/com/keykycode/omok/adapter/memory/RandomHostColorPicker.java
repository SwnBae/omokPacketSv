package com.keykycode.omok.adapter.memory;

import com.keykycode.omok.application.port.out.HostColorPicker;
import com.keykycode.omok.domain.game.Color;

import java.util.concurrent.ThreadLocalRandom;

public class RandomHostColorPicker implements HostColorPicker {

    @Override
    public Color pickHostColor() {
        return ThreadLocalRandom.current().nextBoolean() ? Color.BLACK : Color.WHITE;
    }
}
