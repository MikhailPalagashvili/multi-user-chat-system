package ChatServer.src;
import org.apache.commons.lang3.StringUtils;
import java.io.*;
import java.net.*;
import java.util.*;

public class ServerWorker extends Thread {

    private final Socket clientSocket;
    private final Server server;
    private String login;
    private OutputStream outputStream;
    private HashSet <String> groupChat;

    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
        groupChat = new HashSet <>();
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSocket() throws IOException {
        this.outputStream = clientSocket.getOutputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] tokens = StringUtils.split(line);
            if (tokens != null && tokens.length > 0) {
                String command = tokens[0];
                if ("logoff".equals(command) || "quit".equalsIgnoreCase(command)) {
                    handleLogoff();
                    break;
                } else if ("login".equalsIgnoreCase(command)) {
                    handleLogin(tokens);
                } else if ("msg".equalsIgnoreCase(command)) {
                    String[] tokensMsg = StringUtils.split(line, null, 3);
                    handleMessage(tokensMsg);
                } else if ("join".equalsIgnoreCase(command)) {
                    handleJoin(tokens);
                } else if ("leave".equalsIgnoreCase(command)) {
                    handleLeave(tokens);
                } else {
                    String msg = "unknown " + command + "\n";
                    outputStream.write(msg.getBytes());
                }
            }
        }

        clientSocket.close();
    }

    private void handleLeave(String[] tokens) {
        if (tokens.length > 1) {
            String chatRoom = tokens[1];
            groupChat.remove(chatRoom);
        }
    }

    public boolean isMemberOfGroupChat(String chatRoom) {
        return groupChat.contains(chatRoom);
    }

    private void handleJoin(String[] tokens) {
        if (tokens.length > 1) {
            String chatRoom = tokens[1];
            groupChat.add(chatRoom);
        }
    }

    // format: "message" "login" body...
    // format: "message" "#groupChat" body...
    private void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];
        boolean isGroupChat = sendTo.charAt(0) == '#';
        List <ServerWorker> workers = server.getWorkerList();
        for (ServerWorker worker : workers) {
            if (isGroupChat) {
                if (worker.isMemberOfGroupChat(sendTo)) {
                    String outMsg = "msg " + sendTo + " from " + login + ": " + body + "\n";
                    worker.send(outMsg);
                }
            } else {
                if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                    String outMsg = "msg " + login + " " + body + "\n";
                    worker.send(outMsg);
                }
            }
        }
    }

    private void handleLogoff() throws IOException {
        server.removeWorker(this);
        List <ServerWorker> workers = server.getWorkerList();

        // send other online users current user's status
        String onlineMsg = "offline " + login + "\n";
        for (ServerWorker worker : workers) {
            if (!login.equals(worker.getLogin())) {
                worker.send(onlineMsg);
            }
        }
        clientSocket.close();
    }

    public String getLogin() {
        return login;
    }

    private void handleLogin(String[] tokens) throws IOException {
        if (tokens.length == 3) {
            String login = tokens[1];
            String password = tokens[2];
            boolean valid = verification(login, password);
            if (valid) {
                String msg = "ok login\n";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("User logged in successfully: " + login);

                List <ServerWorker> workers = server.getWorkerList();

                // send current user all other online logins
                for (ServerWorker worker : workers) {
                    if (worker.getLogin() != null) {
                        if (!login.equals(worker.getLogin())) {
                            String msg2 = "online " + worker.getLogin() + "\n";
                            send(msg2);
                        }
                    }
                }

                // send other online users current user's status
                String onlineMsg = "online " + login + "\n";
                for (ServerWorker worker : workers) {
                    if (!login.equals(worker.getLogin())) {
                        worker.send(onlineMsg);
                    }
                }
            } else {
                String msg = "error login\n";
                outputStream.write(msg.getBytes());
                System.err.println("Login failed for " + login);
            }
        }
    }


    private boolean verification(String login, String password) {
        return login.matches("[a-zA-Z0-9]{5,20}") && password.matches("[a-zA-Z0-9]{5,20}");
    }

    private void send(String msg) throws IOException {
        if (login != null)
            outputStream.write(msg.getBytes());
    }
}
