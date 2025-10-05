import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws Exception {
        Interlocking il = new InterlockingImpl();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Interlocking CLI (grader API):");
        System.out.println(" add <id> <entry> <exit>");
        System.out.println(" move <id>");
        System.out.println(" where <id>");
        System.out.println(" sec <section>");
        System.out.println(" quit");
        while (true) {
            System.out.print("> ");
            String line = br.readLine();
            if (line == null)
                break;
            String[] t = line.trim().split("\\s+");
            if (t.length == 0 || t[0].isEmpty())
                continue;
            switch (t[0].toLowerCase()) {
                case "add":
                    if (t.length < 4) {
                        System.out.println("usage: add <id> <entry> <exit>");
                        break;
                    }
                    System.out.println(il.addTrain(t[1], Integer.parseInt(t[2]), Integer.parseInt(t[3])));
                    break;
                case "move":
                    if (t.length < 2) {
                        System.out.println("usage: move <id>");
                        break;
                    }
                    System.out.println(il.moveTrains(new String[] { t[1] }));
                    break;
                case "where":
                    if (t.length < 2) {
                        System.out.println("usage: where <id>");
                        break;
                    }
                    System.out.println(il.getTrain(t[1]));
                    break;
                case "sec":
                    if (t.length < 2) {
                        System.out.println("usage: sec <section>");
                        break;
                    }
                    System.out.println(il.getSection(Integer.parseInt(t[1])));
                    break;
                case "quit":
                case "exit":
                    return;
                default:
                    System.out.println("?");
            }
        }
    }
}
