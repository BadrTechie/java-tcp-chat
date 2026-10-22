package io.github.badrtechie.tcpchat.server.business;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerSettingsTest {
    @TempDir
    Path directory;

    @Test
    void loadsUtf8PropertiesAndAllowsEphemeralBindForTests() throws Exception {
        Path file = directory.resolve("server.properties");
        Files.writeString(file, "server.host=127.0.0.1\nserver.port=0\n");
        assertEquals(new ServerSettings("127.0.0.1", 0), ServerSettings.load(file));
    }

    @Test
    void rejectsMissingHostAndInvalidPort() {
        assertThrows(IllegalArgumentException.class, () -> new ServerSettings(" ", 3000));
        assertThrows(IllegalArgumentException.class, () -> new ServerSettings("localhost", -1));
        assertThrows(IllegalArgumentException.class, () -> new ServerSettings("localhost", 65_536));
    }
}
