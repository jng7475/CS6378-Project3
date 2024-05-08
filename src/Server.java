import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final String id;
    private final int numID;
    private final int ownPort;
    private ServerSocket serverSocket;
    public int[][] matrix = new int[4][4];
    public Set<Integer> connections = new HashSet<>();
    public Set<String> messages = new HashSet<>();
    public CopyOnWriteArrayList<int[]> messageBufferList = new CopyOnWriteArrayList<>();
    public boolean deferHappened = false;
    public int messageSent = 0;
    public int messageReceived = 0;
    public int initializeCounter = 0;
    public int sequence = 0;
    public int lastSequence = -1;
    public ArrayList<Integer> deferredSequence = new ArrayList<>();
    // hash map to store objects
    public HashMap<Integer, Object> objectMap = new HashMap<>();
    List<String> serverIPs = new ArrayList<>();
    List<String> clientIPs = new ArrayList<>();

    public Server(String id, int port, int numID) throws IOException {
        this.id = id;
        this.ownPort = port;
        this.numID = numID;
        this.serverSocket = new ServerSocket(port);
        this.matrix = new int[][] { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } };
        serverIPs.add("10.176.69.33"); // 02
        serverIPs.add("10.176.69.34"); // 03
        serverIPs.add("10.176.69.35"); // 04
        serverIPs.add("10.176.69.36"); // 05
        serverIPs.add("10.176.69.55"); // 24
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
            // updateMatrix(true, null, senderID, false);
            // // print matrix
            // System.out.println("Sending matrix: " + Arrays.deepToString(matrix));
            // for (int i = 0; i < matrix.length; i++) {
            // for (int j = 0; j < matrix[i].length; j++) {
            // message += "," + matrix[i][j];
            // }
            // }
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
            } else if (command.equals("read")) {
                List<String> receivers = new ArrayList<>();
                // add client to receivers
                receivers.add(clientIPs.get(senderID - 1) + ":" + (2100 + senderID));
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
        }
        return;
        // int[][] senderMatrix = new int[4][4];
        // for (int i = 0; i < 4; i++) {
        // for (int j = 0; j < 4; j++) {
        // senderMatrix[i][j] = Integer.parseInt(message[2 + (i * 4) + j]);
        // }
        // }
        // System.out.println("Sender matrix received from " + senderID + " : " +
        // Arrays.deepToString(senderMatrix));
        // String eligibilityCheck = canReceive(senderMatrix, senderID);
        // int problemID = Integer.parseInt(eligibilityCheck.substring(1));
        // boolean eligible = false;
        // if (eligibilityCheck.charAt(0) == 'f') {
        // eligible = false;
        // } else {
        // eligible = true;
        // }
        // if (eligible) {
        // System.out.println("Eligible to receive from " + senderID);
        // updateMatrix(false, senderMatrix, senderID, eligible);
        // System.out.println("Updated matrix after receive: " +
        // Arrays.deepToString(matrix));
        // messageReceived += 1;
        // receiveDeferredMessages(problemID, senderID);
        // } else {
        // System.out.println("deferring");
        // deferMessage(problemID, senderMatrix, senderID);
        // }
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

    public synchronized void updateMatrix(boolean sending, int[][] senderMatrix, int senderID, boolean eligible) {
        if (sending) {
            for (int i = 0; i < matrix.length; i++) {
                if (i == senderID - 1) {
                    for (int j = 0; j < matrix[i].length; j++) {
                        if (j != senderID - 1) {
                            matrix[i][j]++;
                        }
                    }
                }
            }
        } else if (eligible) {
            for (int i = 0; i < matrix.length; i++) {
                if (i == senderID - 1) {
                    for (int j = 0; j < matrix[i].length; j++) {
                        matrix[i][j] = senderMatrix[i][j];
                    }
                }
            }
        }
    }

    public void updateMatrixFromDeferred(int[][] deferredMatrix, int senderID) {
        for (int i = 0; i < matrix.length; i++) {
            if (i == senderID - 1) {
                for (int j = 0; j < matrix[i].length; j++) {
                    matrix[i][j] = deferredMatrix[i][j];
                }
            }
        }
    }

    public void broadcastMessage(List<String> receivers, int id)
            throws InterruptedException, IOException {
        int numberofMessages = 1;
        for (int i = 0; i < numberofMessages; i++) {
            // Wait for a random amount of time in the range (0,10] milliseconds.
            int random = (int) (Math.random() * 10) + 1;
            Thread.sleep(random);
            String message = i + "," + id;
            this.sendMessages(receivers, message, id);
        }
        // exit after sending 100 messages and receiving 300 messages
        while (this.messageSent < numberofMessages || this.messageReceived < numberofMessages * connections.size()) {
            Thread.sleep(100);
        }
        System.out.println("defer happened: " + this.deferHappened);
        System.out.println("message sent: " + this.messageSent);
        System.out.println("message received: " + this.messageReceived);
        System.out.println("Communication is complete");
        System.out.println("Communication is complete");
        System.exit(0);
    }

    public String canReceive(int[][] senderMatrix, int senderID) {
        // 1st condition, only look at numID column and rows other than numID row and
        // senderID
        for (int i = 0; i < matrix.length; i++) {
            if (i != numID - 1 && i != senderID - 1) {
                for (int j = 0; j < matrix[i].length; j++) {
                    if (j == numID - 1) {
                        if (matrix[i][j] < senderMatrix[i][j]) {
                            System.out.println(
                                    "Failed at 1st condition from sender " + senderID + "at problemID " + (i + 1));
                            return "f" + (i + 1);
                        }
                    }
                }
            }
        }
        // 2nd condition, look at senderID and numID row and numID column
        for (int i = 0; i < matrix.length; i++) {
            if (i == senderID - 1) {
                for (int j = 0; j < matrix[i].length; j++) {
                    if (j == numID - 1) {
                        if (matrix[i][j] + 1 != senderMatrix[i][j]) {
                            System.out.println(
                                    "Failed at 2nd condition from sender " + senderID + "at problemID " + (i +
                                            1));
                            return "f" + (i + 1);
                        }
                    }
                }
            }
        }
        return "t0";
    }

    // check if any deferred messages can be received
    public void receiveDeferredMessages(int problemID, int senderID) throws IOException, InterruptedException {
        System.out.println("in receive deferred messages");
        boolean deferredExists = false;
        if (messageBufferList.isEmpty()) {
            return;
        }
        // print before size
        System.out.println("Before size: " + messageBufferList.size());
        do {
            deferredExists = false;
            List<int[]> elementsToRemove = new ArrayList<>();
            Iterator<int[]> iterator = messageBufferList.iterator();
            while (iterator.hasNext()) {
                int[] messageBufferArray = iterator.next();
                // pop each message from messageBufferList
                // for (int i = 0; i < messageBufferList.size(); i++) {
                // int[] messageBufferArray = messageBufferList.get(i);
                int problemIDFromList = messageBufferArray[0];
                int senderIDFromList = messageBufferArray[1];
                int[][] senderMatrixFromList = new int[4][4];
                for (int j = 0; j < 4; j++) {
                    for (int k = 0; k < 4; k++) {
                        senderMatrixFromList[j][k] = messageBufferArray[2 + (j * 4) + k];
                    }
                }
                // print sender matrix
                System.out.println("Sender matrix in defer: " + Arrays.deepToString(senderMatrixFromList) + " from "
                        + senderIDFromList);
                // check if message can be received
                String eligibilityCheck = canReceive(senderMatrixFromList, senderIDFromList);
                boolean eligible = false;
                // if first letter is f, then it failed the condition
                if (eligibilityCheck.charAt(0) == 'f') {
                    eligible = false;
                } else {
                    eligible = true;
                }
                // if eligible, receive message and update matrix to sender matrix
                if (eligible) {
                    System.out.println("Eligible to receive from " + senderIDFromList);
                    updateMatrixFromDeferred(senderMatrixFromList, senderIDFromList);
                    System.out.println("Updated matrix after receive from list: " + Arrays.deepToString(matrix));
                    messageReceived += 1;
                    deferredExists = true;
                    // add to elementsToRemove to remove from messageBufferList
                    elementsToRemove.add(messageBufferArray);
                    deferredExists = true; // Set to true when deferred message is found
                    // call receiveDeferredMessages again
                    // receiveDeferredMessages(problemID, senderID);
                    break;
                    // check if any deferred messages can be received
                }
            }
            messageBufferList.removeAll(elementsToRemove);
            // print after size
            System.out.println("After size: " + messageBufferList.size());
        } while (deferredExists);
        return;
    }

    public void deferMessage(int problemID, int[][] senderMatrix, int senderID) {
        deferHappened = true;
        System.out.println("defer happened: " + deferHappened);
        // add to messageBufferList
        int[] messageBufferArray = new int[18];
        messageBufferArray[0] = problemID;
        messageBufferArray[1] = senderID;
        // add sender matrix to messageBufferArray
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                messageBufferArray[2 + (i * 4) + j] = senderMatrix[i][j];
            }
        }
        messageBufferList.add(messageBufferArray);
        for (int[] message : messageBufferList) {
            System.out.println("Message in messageBufferList: " + Arrays.toString(message));
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