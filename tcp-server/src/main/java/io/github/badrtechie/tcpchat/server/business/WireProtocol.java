package io.github.badrtechie.tcpchat.server.business;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

final class WireProtocol {
    static final int MAX_FRAME_BYTES = 1_048_576;

    enum Type {
        HELLO, WELCOME, CHAT, MESSAGE, USERS_REQUEST, USERS, ERROR, DISCONNECT, SHUTDOWN
    }

    record Frame(Type type, List<String> fields) {
        Frame(Type type, String... fields) {
            this(type, List.of(fields));
        }
    }

    private WireProtocol() {
    }

    static void write(DataOutputStream output, Frame frame) throws IOException {
        StringBuilder encoded = new StringBuilder(frame.type().name());
        Base64.Encoder encoder = Base64.getEncoder();
        for (String field : frame.fields()) {
            encoded.append('\t').append(encoder.encodeToString(field.getBytes(StandardCharsets.UTF_8)));
        }
        byte[] bytes = encoded.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FRAME_BYTES) {
            throw new IOException("Protocol frame is too large.");
        }
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

    static Frame read(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 1 || length > MAX_FRAME_BYTES) {
            throw new IOException("Invalid protocol frame length: " + length);
        }
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) {
            throw new IOException("Incomplete protocol frame.");
        }
        String[] parts = new String(bytes, StandardCharsets.UTF_8).split("\\t", -1);
        try {
            Type type = Type.valueOf(parts[0]);
            Base64.Decoder decoder = Base64.getDecoder();
            List<String> fields = new ArrayList<>();
            for (int index = 1; index < parts.length; index++) {
                fields.add(new String(decoder.decode(parts[index]), StandardCharsets.UTF_8));
            }
            return new Frame(type, List.copyOf(fields));
        } catch (IllegalArgumentException ex) {
            throw new IOException("Malformed protocol frame.", ex);
        }
    }
}
