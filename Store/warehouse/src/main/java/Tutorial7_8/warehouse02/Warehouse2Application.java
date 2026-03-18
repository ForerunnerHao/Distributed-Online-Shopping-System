package Tutorial7_8.warehouse02;


import Tutorial7_8.warehouse.config.DatabaseInitializer;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@PropertySource("classpath:application-warehouse2.properties")
@SpringBootApplication(scanBasePackages = "Tutorial7_8.warehouse")
@EnableJpaRepositories(basePackages = "Tutorial7_8.warehouse")
@EntityScan("Tutorial7_8.warehouse")
public class Warehouse2Application {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Warehouse2Application.class)
                .listeners(new DatabaseInitializer("warehouse2"))
                .web(WebApplicationType.NONE)
                .run(args);
    }
}