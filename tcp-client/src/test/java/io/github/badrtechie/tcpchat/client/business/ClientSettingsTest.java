package io.github.badrtechie.tcpchat.client.business;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientSettingsTest {
    @TempDir
    Path directory;

    @Test
    void loadsPropertiesWhenArgumentsAreAbsent() throws Exception {
        Path file = properties("configured.example", 4100);
        assertEquals(new ClientSettings("configured.example", 4100),
                ClientSettings.resolve(file, new String[0]));
    }

    @Test
    void twoArgumentsOverridePropertiesWithoutReadingThem() throws Exception {
        Path missing = directory.resolve("missing.properties");
        assertEquals(new ClientSettings("cli.example", 4200),
                ClientSettings.resolve(missing, new String[]{"cli.example", "4200"}));
    }

    @Test
    void rejectsPartialArgumentsAndInvalidInputs() throws Exception {
        Path file = properties("configured.example", 4100);
        assertThrows(IllegalArgumentException.class,
                () -> ClientSettings.resolve(file, new String[]{"only-host"}));
        assertThrows(IllegalArgumentException.class,
                () -> ClientSettings.resolve(file, new String[]{"host", "0"}));
        assertThrows(IllegalArgumentException.class,
                () -> ClientSettings.resolve(file, new String[]{"bad host", "3000"}));
    }

    private Path properties(String host, int port) throws Exception {
        Path file = directory.resolve("client.properties");
        Files.writeString(file, "server.host=" + host + "\nserver.port=" + port + "\n");
        return file;
    }
}
