package Tutorial7_8.Store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import io.swagger.v3.oas.models.OpenAPI;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class Swagger {
    @Value("${openapi.dev-url}")
    private String devUrl;
    @Value("${openapi.prod-url}")
    private String prodUrl;

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Server URL in Development environment");
        Info info = new Info()
                .title("Online Store API")
                .version("1.0")
                .description("API Description.");
        return new OpenAPI().info(info).servers(List.of(devServer));
    }
}

