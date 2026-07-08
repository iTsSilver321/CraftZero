package com.craftzero.multiplayer;

import java.io.BufferedReader;
import java.io.IOException;

final class ProtocolLine {
    private ProtocolLine() {
    }

    static String read(BufferedReader reader) throws IOException {
        if (reader == null) {
            return null;
        }
        StringBuilder builder = null;
        int value;
        while ((value = reader.read()) != -1) {
            if (value == '\n') {
                return builder == null ? "" : builder.toString();
            }
            if (value == '\r') {
                continue;
            }
            if (builder == null) {
                builder = new StringBuilder();
            }
            if (builder.length() >= MultiplayerProtocol.MAX_PROTOCOL_LINE_LENGTH) {
                throw new IOException("Protocol line too long");
            }
            builder.append((char) value);
        }
        return builder == null ? null : builder.toString();
    }

    static void validateEncoded(String line) throws IOException {
        if (line == null || line.length() > MultiplayerProtocol.MAX_PROTOCOL_LINE_LENGTH) {
            throw new IOException("Protocol line too long");
        }
    }
}
