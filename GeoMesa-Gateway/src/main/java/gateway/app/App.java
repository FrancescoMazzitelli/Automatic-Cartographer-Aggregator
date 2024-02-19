package gateway.app;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;


@SpringBootApplication
@ComponentScan(basePackages = "gateway")
public class App {

    public static void checkDirectory(String dirPath) {
        File dir = new File(dirPath);

        if (!dir.exists()) {
            boolean op = dir.mkdirs();

            if (op) {
                System.out.println("Cartella creata: " + dirPath);
            } else {
                System.out.println("Impossibile creare la cartella: " + dirPath);
            }
        }
    }

    public static void main(String[] args) {
        String dirPath1 = "Shapefiles";
        checkDirectory(dirPath1);

        String dirPath2 = "Computed";
        checkDirectory(dirPath2);

       SpringApplication.run(App.class, args);
    }
}
