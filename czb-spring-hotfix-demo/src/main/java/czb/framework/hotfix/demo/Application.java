package czb.framework.hotfix.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages ="czb.framework.hotfix.demo.mapper" )
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
