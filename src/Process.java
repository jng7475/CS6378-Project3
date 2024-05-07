import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Process {
    public static void main(String[] args) throws Exception {
        // args[0] will be of form s1,s2,s3,s4,s5,s6,s7 or c1,c2,c3,c4,c5
        String arg = args[0];
        boolean isServer = arg.charAt(0) == 's';
        int id = Integer.parseInt(arg.substring(1));
        String processId = "Process " + id;
        int port = 2100 + id;
        List<String> serverIPs = new ArrayList<>();
        // serverIPs.add("10.176.69.32");
        serverIPs.add("10.176.69.33");
        serverIPs.add("10.176.69.34");
        serverIPs.add("10.176.69.35");
        serverIPs.add("10.176.69.36");
        serverIPs.add("10.176.69.55");
        serverIPs.add("10.176.69.56");
        serverIPs.add("10.176.69.62");
        if (isServer) {
            System.out.println("Starting server " + processId + " on port " + port);
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
            // process.broadcastMessage(receivers, id);
        } else {
            Client process = new Client(processId, port, id);
            Thread receiverThread = new Thread(() -> {
                try {
                    process.receiveMessages();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            receiverThread.start();
            Thread.sleep(1000);
            // send 1 message to a random server
            List<String> receivers = new ArrayList<>();
            for (int i = 1; i < 8; i++) {
                String IP = serverIPs.get(i - 1);
                receivers.add(IP + ":" + (2100 + i));

            }
            process.sendMessages(receivers, "client" + "," + id + "," + "update", id);
        }
    }
}
