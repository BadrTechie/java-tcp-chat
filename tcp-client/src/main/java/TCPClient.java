import io.github.badrtechie.tcpchat.client.ClientBootstrap;

public final class TCPClient {
    private TCPClient() {
    }

    public static void main(String[] args) {
        ClientBootstrap.start(args);
    }
}
