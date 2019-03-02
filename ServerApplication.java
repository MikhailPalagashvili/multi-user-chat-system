package ChatServer.src;
public class ServerApplication {
    public static void main(String[] args) {
        final int port = 2111;
        final Server server = new Server(port);
        server.start();
    }
}

