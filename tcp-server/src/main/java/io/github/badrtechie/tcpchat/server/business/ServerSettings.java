package io.github.badrtechie.tcpchat.server.business;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record ServerSettings(String host, int port) {
    public static final Path DEFAULT_PATH = Path.of("config", "server.properties");

    public ServerSettings {
        host = Inputs.requireHost(host);
        port = Inputs.requireBindPort(port);
    }

    public static ServerSettings load(Path path) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        String portText = properties.getProperty("server.port");
        if (portText == null) {
            throw new IllegalArgumentException("Missing required property: server.port");
        }
        return new ServerSettings(properties.getProperty("server.host"), Inputs.parseBindPort(portText));
    }
}
