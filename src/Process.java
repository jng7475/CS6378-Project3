import java.util.*;

public class Process {
    public static int[] hashFunction(int key) {
        // hashResult array with 3 values: key % 7, (key+2) %7, (key+4) % 7
        int[] hashResult = new int[3];
        hashResult[0] = key % 7;
        hashResult[1] = (key + 2) % 7;
        hashResult[2] = (key + 4) % 7;
        return hashResult;
    }

    public static void main(String[] args) throws Exception {
        // args[0] will be of form s1,s2,s3,s4,s5,s6,s7 or c1,c2,c3,c4,c5
        String arg = args[0];
        boolean isServer = arg.charAt(0) == 's';
        String status = "";
        // if there's a second argument, it will be the status of the process
        if (args.length > 1) {
            status = args[1];
        }
        System.out.println("Status: " + status);
        int id = Integer.parseInt(arg.substring(1));
        String processId = "Process " + id;
        int port = 2100 + id;

        if (isServer) {
            System.out.println("Starting server " + processId + " on port " + port);

            Server process = new Server(processId, port, id);

            List<String> receivers = new ArrayList<>();
            for (int i = 1; i < 8; i++) {
                if (i != id) {
                    // 31 - 34 = dc01 - dc04
                    String IP = process.serverIPs.get(i - 1);
                    receivers.add(IP + ":" + (2100 + i));
                }
            }
            Thread receiverThread = new Thread(() -> {
                try {
                    process.receiveMessages();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            receiverThread.start();
            Thread.sleep(1000 * (8 - id));
            if (status.equals("i")) {
                process.sendMessages(receivers, "initializing" + "," + id, id);
                while (process.connections.size() < 6) {
                    Thread.sleep(100);
                }
            } else if (status.equals("r")) {
                process.sendMessages(receivers, "recover" + "," + id, id);
            }
            Thread.sleep(1000);
            if (status.equals("r")) {
                // print current object maps
                for (int key : process.objectMap.keySet()) {
                    // print object ID and content
                    System.out.println("Object ID: " + key + ", Content: " + process.objectMap.get(key).content);
                }
            }
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
            // continuosly get input from user
            while (true) {
                Scanner scanner = new Scanner(System.in);
                System.out.println("Enter command: ");
                // command will be of form write 1, delete 1, search 1
                String command = scanner.nextLine();
                String[] commandArr = command.split(" ");
                String commandType = commandArr[0];
                String objectID = commandArr[1];
                switch (commandType) {
                    case "write":
                        process.write(Integer.parseInt(objectID));
                        break;
                    case "read":
                        process.read(Integer.parseInt(objectID));
                        break;
                    default:
                        System.out.println("Invalid command");
                        break;
                }
            }
        }
    }
}
