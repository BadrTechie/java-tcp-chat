import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TCPServerTest {
    @Test
    void printsMilestonePlaceholder() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            TCPServer.main(new String[0]);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(
                "TCPServer setup is ready; server networking and JavaFX are not implemented yet."
                        + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8)
        );
    }
}
