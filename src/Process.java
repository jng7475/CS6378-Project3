import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Process {
    public static void main(String[] args) throws Exception {
        int id = Integer.parseInt(args[0]);
        String processId = "Process " + id;
        int port = 2100 + id;
        System.out.println("Starting " + processId + " on port " + port);
        List<String> serverIPs = new ArrayList<>();
        // serverIPs.add("10.176.69.32");
        serverIPs.add("10.176.69.33");
        serverIPs.add("10.176.69.34");
        serverIPs.add("10.176.69.35");
        serverIPs.add("10.176.69.36");
        serverIPs.add("10.176.69.55");
        serverIPs.add("10.176.69.56");
        serverIPs.add("10.176.69.62");
        List<String> receivers = new ArrayList<>();
        for (int i = 1; i < 8; i++) {
            if (i != id) {
                // 31 - 34 = dc01 - dc04
                String IP = serverIPs.get(i - 1);
                receivers.add(IP + ":" + (2100 + i));
            }
        }
        // print receivers
        for (String receiver : receivers) {
            System.out.println("Receiver: " + receiver);
        }
        Server process = new Server(processId, port, id);
        Thread receiverThread = new Thread(() -> {
            try {
                process.receiveMessages();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        receiverThread.start();
        Thread.sleep(1000 * (8 - id));
        process.sendMessages(receivers, "initializing" + "," + id, id);
        while (process.connections.size() < 6) {
            Thread.sleep(100);
        }
        Thread.sleep(1000);

        process.broadcastMessage(receivers, id);
    }
}
