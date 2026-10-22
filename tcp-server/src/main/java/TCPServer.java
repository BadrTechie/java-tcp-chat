import io.github.badrtechie.tcpchat.server.ServerBootstrap;

public final class TCPServer {
    private TCPServer() {
    }

    public static void main(String[] args) {
        ServerBootstrap.start(args);
    }
}
