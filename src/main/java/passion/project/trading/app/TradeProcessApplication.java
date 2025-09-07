package passion.project.trading.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import passion.project.trading.controller.TradeController;

@SpringBootApplication
@ComponentScan(basePackageClasses= TradeController.class)
public class TradeProcessApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeProcessApplication.class, args);
    }
}
