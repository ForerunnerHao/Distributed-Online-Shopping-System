package Tutorial7_8.warehouse01;


import Tutorial7_8.warehouse.config.DatabaseInitializer;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@PropertySource("classpath:application-warehouse1.properties")
@SpringBootApplication(scanBasePackages = "Tutorial7_8.warehouse")
@EnableJpaRepositories(basePackages = "Tutorial7_8.warehouse")
@EntityScan("Tutorial7_8.warehouse")
public class Warehouse1Application {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Warehouse1Application.class)
                .listeners(new DatabaseInitializer("warehouse1"))
                .web(WebApplicationType.NONE)
                .run(args);
    }
}