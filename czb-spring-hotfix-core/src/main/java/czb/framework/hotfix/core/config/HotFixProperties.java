package czb.framework.hotfix.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;
import java.util.List;

/**
 * 热修复参数配置
 */
@ConfigurationProperties("hotfix")
public class HotFixProperties {

    /**
     * 是否开启热修复
     */
    private Boolean enable=false;
    /**
     * 基础包名
     */
    private String basePackage;

    /**
     * 本地文件加载地址
     * <p>loadPath后面必须是包名路径，class文件不可以随便乱放，必须要按照包名路径存放。</p>
     * <p>mapper层的 mapper.xml必须放在对应 mapper接口的class文件同一目录下</p>
     */
    private String loadPath;

    /**
     * 需要加载到AppClassLoader【父级ClassLoader】的包名,
     * <p>一般 热修复类调用一个新热修复类时，需要将其加载到父级类加载中，比如 entity层，vo层。因为一个热修复类其实是对应
     * 一个ClassLoader，这就导致了如果这个热修复类要调用一个新热修复类时，会报错 NoClassDefFoundError 。加载到父级ClassLoader
     * 就可以防止这个异常的发生。</p>
     * <p>名称说明：
     *  <ol>新热修复类:AppClassLoader没有加载过的类，但是通过热修复加载进来了的类。</ol>
     * </p>
     */
    private List<String> shouldLoadInAppClassLoaderPackage;

    public List<String> getShouldLoadInAppClassLoaderPackage() {
        return shouldLoadInAppClassLoaderPackage;
    }

    public void setShouldLoadInAppClassLoaderPackage(List<String> shouldLoadInAppClassLoaderPackage) {
        this.shouldLoadInAppClassLoaderPackage = shouldLoadInAppClassLoaderPackage;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * 如果 {@link #loadPath} 不是 {@link File#separator} 结尾，会自动加上 {@link File#separator}
     */
    public String getLoadPath() {
        if(loadPath.endsWith(File.separator)){
            return loadPath;
        }else{
            loadPath=loadPath+File.separator;
            return loadPath;
        }
    }

    public void setLoadPath(String loadPath) {
        this.loadPath = loadPath;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }
}
