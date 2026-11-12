package com.keykycode.omok.adapter.tcp.dto;

public sealed interface DecodeResult {

    record Incomplete() implements DecodeResult {
    }

    record Decoded(Request request) implements DecodeResult {
    }

    record Malformed() implements DecodeResult {
    }
}
