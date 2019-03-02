package ChatServer.src;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server extends Thread {
    private final int serverPort;
    private final ArrayList <ServerWorker> workers;

    public Server(final int serverPort) {
        this.serverPort = serverPort;
        this.workers = new ArrayList <>();
    }

    public List <ServerWorker> getWorkerList() {
        return workers;
    }

    @Override
    public void run() {
        try {
            /* serverSocket which waits for client requests on a specific port*/
            ServerSocket serverSocket = new ServerSocket(serverPort);
            while (true) {
                System.out.println("About to accept client connection...");
                /* plain old socket to use for bidirectional communication with the client on a different port */
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket);
                /* each serverWorker handles the socket connection between the main server and its client */
                ServerWorker worker = new ServerWorker(this, clientSocket);
                workers.add(worker);
                /* starts a thread for each communication with the client */
                worker.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeWorker(ServerWorker serverWorker) {
        workers.remove(serverWorker);
    }
}

