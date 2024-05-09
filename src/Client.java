import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private final String id;
    private final int numID;
    private final int ownPort;
    private ServerSocket serverSocket;
    public int[][] matrix = new int[4][4];
    public Set<Integer> connections = new HashSet<>();
    public Set<String> messages = new HashSet<>();
    public boolean deferHappened = false;
    public int messageSent = 0;
    public int messageReceived = 0;
    public int initializeCounter = 0;
    public int sequence = 0;
    public int lastSequence = -1;
    public ArrayList<Integer> deferredSequence = new ArrayList<>();
    List<String> serverIPs = new ArrayList<>();
    List<String> clientIPs = new ArrayList<>();
    public boolean readSuccess = false;
    public boolean writeRequestSuccess = false;
    public boolean writeFailed = false;

    public Client(String id, int port, int numID) throws IOException {
        this.id = id;
        this.ownPort = port;
        this.numID = numID;
        this.serverSocket = new ServerSocket(port);
        this.matrix = new int[][] { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } };
        serverIPs.add("10.176.69.33"); // 02
        serverIPs.add("10.176.69.37"); // 06
        serverIPs.add("10.176.69.35"); // 04
        serverIPs.add("10.176.69.36"); // 05
        serverIPs.add("10.176.69.54"); // 23
        serverIPs.add("10.176.69.56"); // 25
        serverIPs.add("10.176.69.62"); // 31

        clientIPs.add("10.176.69.72"); // 41
        clientIPs.add("10.176.69.73"); // 42
        clientIPs.add("10.176.69.71"); // 40
        clientIPs.add("10.176.69.75"); // 44
        clientIPs.add("10.176.69.76"); // 45
    }

    public void sendMessages(List<String> receivers, String message, int senderID)
            throws IOException, InterruptedException {
        System.out.println("Sending message: " + message);
        String[] sendingMessage = message.split(",");
        if (!sendingMessage[0].equals("initializing")) {
            messageSent += 1;
        }
        // try{
        byte[] data = message.getBytes();
        for (String receiver : receivers) {
            int receiverPort = Integer.parseInt(receiver.split(":")[1]);
            String receiverAddress = receiver.split(":")[0];
            Socket socket = new Socket(receiverAddress, receiverPort);
            OutputStream out = socket.getOutputStream();
            out.write(data);
            out.flush();
            socket.close();
        }

    }

    public void processMessages(InputStream in) throws InterruptedException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String receivedMessage = reader.readLine();
        if (receivedMessage == null || receivedMessage.isEmpty()) {
            return;
        }
        int delay = (int) (Math.random() * 5) + 1;
        Thread.sleep(delay);
        String[] message = receivedMessage.split(",");
        int senderID = Integer.parseInt(message[1]);

        if (message[0].equals("initializing")) {
            connections.add(senderID);
            System.out.println("Connected to " + senderID);
            return;
        }
        if (message[0].equals("success")) {
            int serverID = Integer.parseInt(message[1]);
            String command = message[2];
            int content = Integer.parseInt(message[3]);
            if (command.equals("write")) {
                writeRequestSuccess = true;
            } else if (command.equals("read")) {
                System.out.println("Read from server " + (serverID - 1) + ": " + content);
                readSuccess = true;
            }
            return;
        } else if (message[0].equals("error")) {
            int serverID = Integer.parseInt(message[1]);
            String error = message[2];
            System.out.println("Failed to read from server " + (serverID - 1) + " with error: " + error);
            return;
        } else if (message[0].equals("errorWrite")) {
            int serverID = Integer.parseInt(message[1]);
            String error = message[2];
            System.out.println("Notice from server " + serverID + " with error: " + error);
            writeFailed = true;
            return;
        }
    }

    // listening messages from other processes
    public void receiveMessages() throws IOException, InterruptedException {
        while (true) {
            Socket clientSocket = serverSocket.accept();
            InputStream in = clientSocket.getInputStream();
            processMessages(in);
            clientSocket.close();
        }
    }

    public static int[] hashFunction(int key) {
        // hashResult array with 3 values: key % 7, (key+2) %7, (key+4) % 7
        int[] hashResult = new int[3];
        hashResult[0] = key % 7;
        hashResult[1] = (key + 2) % 7;
        hashResult[2] = (key + 4) % 7;
        return hashResult;
    }

    public void write(int objectID) throws NumberFormatException, IOException, InterruptedException {
        int[] hashResult = hashFunction(objectID);
        List<String> receivers = new ArrayList<>();
        receivers.add(this.serverIPs.get(hashResult[0]) + ":" + (2100 + hashResult[0] + 1));
        // this.sendMessages(receivers, "client" + "," + numID + "," + "write" + "," +
        // objectID, numID);
        writeRequestSuccess = false;
        writeFailed = false;
        int numDown = 0;
        while (!writeRequestSuccess && numDown < 2 && !writeFailed) {
            try {
                int currentHashServer = hashResult[0];
                if (numDown > 0) {
                    // change server in serverList
                    currentHashServer = hashResult[1];
                    receivers.clear();
                    receivers.add(this.serverIPs.get(hashResult[1]) + ":" + (2100 + hashResult[1] + 1));
                }
                this.sendMessages(receivers,
                        "client" + "," + numID + "," + "write" + "," + objectID + "," + currentHashServer, numID);
                Thread.sleep(1000);
            } catch (ConnectException e) {
                System.out.println("Server " + hashResult[0] + " is down. Retrying with the next server...");
                numDown++;
            }
        }
        if (!writeRequestSuccess || writeFailed) {
            System.out.println("Write failed. 2/3 servers are down.");
        }
    }

    public void read(int objectID) throws NumberFormatException, IOException, InterruptedException {
        int[] hashResult = hashFunction(objectID);
        List<String> serverList = new ArrayList<>();
        // add randomly from hashResult
        int random = (int) (Math.random() * 3);
        serverList.add(this.serverIPs.get(hashResult[random]) + ":" + (2100 + hashResult[random] + 1));
        // this.sendMessages(serverList, "client" + "," + numID + "," + "read" + "," +
        // objectID, numID);
        int attempt = 0;
        readSuccess = false;
        while (!readSuccess) {
            try {
                Thread.sleep(200);
                if (attempt > 0) {
                    // change server in serverList
                    serverList.clear();
                    int currentServer = random;
                    while (random == currentServer) {
                        random = (int) (Math.random() * 3);
                    }
                    serverList.add(this.serverIPs.get(hashResult[random]) + ":" + (2100 + hashResult[random] + 1));
                }
                this.sendMessages(serverList, "client" + "," + numID + "," + "read" + "," + objectID, numID);
                break;
            } catch (ConnectException e) {
                attempt++;
                System.out.println("Connection refused. Retrying...");
            }
        }

    }

}