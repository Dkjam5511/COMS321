import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Decoder {
    public static void main(String[] args) {
        try{
            File file = new File(args[0]);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                System.out.println(data);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}