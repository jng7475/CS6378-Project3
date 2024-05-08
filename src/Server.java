import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final String id;
    private final int numID;
    private final int ownPort;
    private ServerSocket serverSocket;
    public Set<Integer> connections = new HashSet<>();
    public Set<String> messages = new HashSet<>();
    public int messageSent = 0;
    // hash map to store objects
    public HashMap<Integer, Object> objectMap = new HashMap<>();
    List<String> serverIPs = new ArrayList<>();
    List<String> clientIPs = new ArrayList<>();

    public Server(String id, int port, int numID) throws IOException {
        this.id = id;
        this.ownPort = port;
        this.numID = numID;
        this.serverSocket = new ServerSocket(port);
        serverIPs.add("10.176.69.33"); // 02
        serverIPs.add("10.176.69.34"); // 03
        serverIPs.add("10.176.69.35"); // 04
        serverIPs.add("10.176.69.36"); // 05
        serverIPs.add("10.176.69.54"); // 23
        serverIPs.add("10.176.69.56"); // 25
        serverIPs.add("10.176.69.62"); // 31

        clientIPs.add("10.176.69.72"); // 41
        clientIPs.add("10.176.69.73"); // 42
    }

    public void sendMessages(List<String> receivers, String message, int senderID)
            throws IOException, InterruptedException {
        System.out.println("Sending message: " + message);
        String[] sendingMessage = message.split(",");
        if (!sendingMessage[0].equals("initializing")) {
            messageSent += 1;
        }

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
        } else if (message[0].equals("client")) {
            String command = message[2];
            String objectID = message[3];
            String currentServer = message[4];
            System.out.println("This is main server, from client: " + senderID + " with command: " + command
                    + " and objectID: " + objectID);
            if (command.equals("write")) {
                int[] hashResult = hashFunction(Integer.parseInt(objectID));
                List<String> receivers = new ArrayList<>();
                List<String> receiver1 = new ArrayList<>();
                List<String> receiver2 = new ArrayList<>();
                // add 2nd and 3rd server to receivers
                for (int i = 0; i < 3; i++) {
                    if (hashResult[i] != Integer.parseInt(currentServer)) {
                        // check alive or not
                        // if receiver 1 empty, add to receiver 1, else add to receiver 2
                        if (receiver1.isEmpty()) {
                            receiver1.add(serverIPs.get(hashResult[i]) + ":" + (2100 + hashResult[i] + 1));
                        } else {
                            receiver2.add(serverIPs.get(hashResult[i]) + ":" + (2100 + hashResult[i] + 1));
                        }
                    }
                }
                // send to LIVE servers
                int numDown = 0;
                try {
                    this.sendMessages(receiver1, "test" + "," + numID + "," + "write" + "," + objectID,
                            numID);
                    receivers.add(receiver1.get(0));
                } catch (ConnectException e) {
                    System.out.println("Server " + receiver1.get(0) + " is down");
                    numDown++;
                }

                try {
                    this.sendMessages(receiver2, "test" + "," + numID + "," + "write" + "," + objectID,
                            numID);
                    receivers.add(receiver2.get(0));

                } catch (ConnectException e) {
                    System.out.println("Server " + receiver2.get(0) + " is down");
                    numDown++;

                }

                List<String> clientReceiver = new ArrayList<>();
                // add client to receivers
                clientReceiver.add(clientIPs.get(senderID - 1) + ":" + (2100 + senderID));
                // NEW
                try {
                    if (numDown == 2) {
                        // send error message to client
                        this.sendMessages(clientReceiver,
                                "errorWrite" + "," + numID + ","
                                        + "Write operation failed because 2 or more servers are down",
                                numID);
                        return;
                    }

                    if (objectMap.containsKey(Integer.parseInt(objectID))) {
                        // add 1 to content
                        objectMap.get(Integer.parseInt(objectID)).content += 1;
                    } else {
                        // write object into hash map
                        objectMap.put(Integer.parseInt(objectID), new Object(Integer.parseInt(objectID), 1));
                    }

                    this.sendMessages(receivers, "write" + "," + numID + "," + "write" + "," + objectID,
                            numID);
                    this.sendMessages(clientReceiver,
                            "success" + "," + numID + "," + "write" + ","
                                    + objectMap.get(Integer.parseInt(objectID)).content,
                            numID);
                    // print objectID and object content of current object
                    System.out.println(
                            "ObjectID: " + objectID + " Content: " + objectMap.get(Integer.parseInt(objectID)).content);
                } catch (ConnectException e) {
                    System.out.println("Connection Down");
                }
            } else if (command.equals("read")) {
                List<String> receivers = new ArrayList<>();
                // add client to receivers
                receivers.add(clientIPs.get(senderID - 1) + ":" + (2100 + senderID));
                // NEW
                try {
                    if (objectMap.containsKey(Integer.parseInt(message[3]))) {
                        System.out.println(
                                "ObjectID: " + message[2] + " Content: "
                                        + objectMap.get(Integer.parseInt(message[3])).content);
                        this.sendMessages(receivers,
                                "success" + "," + numID + "," + "read" + ","
                                        + objectMap.get(Integer.parseInt(message[3])).content,
                                numID);
                    } else {
                        System.out.println("ObjectID: " + message[3] + " does not exist");
                        this.sendMessages(receivers,
                                "error" + "," + numID + "," + "Read operation failed because object does not exist",
                                numID);
                    }
                } catch (ConnectException e) {
                    System.out.println("Connection Down");
                }
            }
            return;
        }
        if (message[0].equals("write")) {
            String objectID = message[3];
            if (objectMap.containsKey(Integer.parseInt(objectID))) {
                // add 1 to content
                objectMap.get(Integer.parseInt(objectID)).content += 1;
            } else {
                // write object into hash map
                objectMap.put(Integer.parseInt(objectID), new Object(Integer.parseInt(objectID), 1));
            }
            // print objectID and object content of current object
            System.out.println(
                    "ObjectID: " + objectID + " Content: " + objectMap.get(Integer.parseInt(objectID)).content);
            return;
        }
        if (message[0].equals("recover")) {
            System.out.println("Recovering for server " + (senderID - 1));
            // go through object map, if objectID % 7 = serverID or objectID+2 % 7 =
            // serverID or objectID +4 % 7 = serverID, print objectID and content
            for (Map.Entry<Integer, Object> entry : objectMap.entrySet()) {
                int objectID = entry.getKey();
                int[] hashResult = hashFunction(objectID);
                for (int i = 0; i < 3; i++) {
                    System.out.println("Checking hash " + hashResult[i]);
                    ArrayList<String> receiver = new ArrayList<>();
                    receiver.add(serverIPs.get(senderID - 1) + ":" + (2100 + senderID));
                    if (hashResult[i] == senderID - 1) {
                        System.out.println("ObjectID: " + objectID + " Content: " + entry.getValue().content);
                        this.sendMessages(receiver, "recoverResult" + "," + objectID + "," + entry.getValue().content,
                                numID);
                    }
                }
            }
            return;
        }
        if (message[0].equals("recoverResult")) {
            int objectID = Integer.parseInt(message[1]);
            int content = Integer.parseInt(message[2]);
            if (objectMap.containsKey(objectID)) {
                objectMap.get(objectID).content = content;
            } else {
                objectMap.put(objectID, new Object(objectID, content));
            }
            return;
        }
        return;
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
}