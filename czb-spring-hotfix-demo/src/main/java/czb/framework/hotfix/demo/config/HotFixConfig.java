package czb.framework.hotfix.demo.config;

import czb.framework.hotfix.core.HotFix;
import czb.framework.hotfix.core.config.HotFixParams;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class HotFixConfig {

    @Bean
    public HotFix hotFix(){
        HotFixParams hotFixParams=new HotFixParams();
        //本地文件加载地址
        hotFixParams.setLoadPath("E:\\Project\\Java\\OpenSource\\czb-spring-hotfix\\czb-spring-hotfix-demo\\hotfix");
        //基础包名
        hotFixParams.setBasePackage("czb.framework.hotfix.demo");
        //需要加载到AppClassLoader【父级ClassLoader】的包名
        hotFixParams.setShouldLoadInAppClassLoaderPackage(Arrays.asList(
                "czb.framework.hotfix.demo.entity",
                "czb.framework.hotfix.demo.vo.resq"));
        return new HotFix(hotFixParams);
    }
}
