package czb.framework.hotfix.autoconfigure;

import czb.framework.hotfix.core.HotFix;
import czb.framework.hotfix.core.config.HotFixProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HotFixProperties.class)
@ConditionalOnClass(HotFix.class)
public class HotFixAutoConfiguration {

    @Autowired
    private HotFixProperties hotFixProperties;

    @ConditionalOnProperty(prefix = "hotfix",name="enable",havingValue = "true")
    @Bean
    public HotFix hotFix(){
        return new HotFix(hotFixProperties);
    }
}
