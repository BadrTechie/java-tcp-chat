package io.github.badrtechie.tcpchat.client.business;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record ClientSettings(String host, int port) {
    public static final Path DEFAULT_PATH = Path.of("config", "client.properties");

    public ClientSettings {
        host = Inputs.requireHost(host);
        port = Inputs.requirePort(port);
    }

    public static ClientSettings resolve(Path path, String[] args) throws IOException {
        if (args.length == 2) {
            return new ClientSettings(args[0], Inputs.parsePort(args[1]));
        }
        if (args.length != 0) {
            throw new IllegalArgumentException("Usage: java -jar TCPClient.jar [serverHost serverPort]");
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        String portText = properties.getProperty("server.port");
        if (portText == null) {
            throw new IllegalArgumentException("Missing required property: server.port");
        }
        return new ClientSettings(properties.getProperty("server.host"), Inputs.parsePort(portText));
    }
}
