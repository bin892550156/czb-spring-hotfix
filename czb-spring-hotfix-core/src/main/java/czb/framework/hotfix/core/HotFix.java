package czb.framework.hotfix.core;

import czb.framework.hotfix.core.classloader.HotFixClassLoader;
import czb.framework.hotfix.core.config.HotFixParams;
import czb.framework.hotfix.core.exception.HotFixException;
import czb.framework.hotfix.core.helper.RefNewBeanHelper;
import czb.framework.hotfix.core.strategy.HotFixBeanGenerator;
import czb.framework.hotfix.core.strategy.impl.DefaultHotFixBeanGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 热修复main类
 * @author chenzhuobin
 */
public class HotFix implements ApplicationContextAware {

    private Logger log= LoggerFactory.getLogger(HotFix.class);

    /**
     * 当前应用程序上下文
     */
    private ApplicationContext applicationContext;

    /**
     * 热修复参数配置
     */
    private HotFixParams hotFixParams;

    /**
     * 引用新 Bean 对象【热修复 Bean 对象】到依赖该原 Bean 对象的 Bean 对象 的帮助类
     */
    private RefNewBeanHelper refNewBeanHelper;

    /**
     * 热修复Bean对象生成器，默认为 {@link DefaultHotFixBeanGenerator}
     */
    private HotFixBeanGenerator hotFixBeanGenerator;

    /**
     * 新建一个 HotFix 实例，建议使用配置成单例Bean对象。
     * @param hotFixParams 热修复参数配置
     */
    public HotFix(HotFixParams hotFixParams) {
        this.hotFixParams = hotFixParams;
        this.refNewBeanHelper=new RefNewBeanHelper();
    }

    /**
     * 启动热修复
     */
    public void exec(){
        if(log.isInfoEnabled()){
            log.info(" hotfix start ... ");
        }
        Properties properties = loadHofixClassMapProp();
        DefaultListableBeanFactory beanFactory = getBeanFactory();
        //加载需要热部署的类加载器
        HotFixClassLoader hotFixClassLoader=new HotFixClassLoader(HotFix.class.getClassLoader(), hotFixParams);
        //需要热修复的类的类名集合
        Set<String> hotFixClassNameList = hotFixClassLoader.getClassLoaderMap().keySet();
        //存放 实例化后的需要热修复的Bean映射关系，key=hotFixBeanName,value=hotFixBeanName对应的已经初始化的Bean
        Map<String,Object> hotFixMap=new HashMap<>(hotFixClassNameList.size());
        for (String hotFixClassName : hotFixClassNameList) {
            try {
                //加载要热修复的类，并实例化和使用 Spring 初始化它
                Class<?> hotFixClass = hotFixClassLoader.loadClass(hotFixClassName);
                if(log.isInfoEnabled()){
                    log.info("generate class name [{}]..",hotFixClassName);
                }
                Object hotFixObj=getHotFixBeanGenerator(beanFactory).generate(hotFixClass);
                if(hotFixObj==null) continue;
                hotFixMap.put(hotFixClassName,hotFixObj);
            } catch (ClassNotFoundException e) {
                if(log.isWarnEnabled()){
                    log.warn(" className = {} not found ",hotFixClassName,e);
                }
            }
        }
        //热修复的类有可能会有互相依赖的情况，这里对依赖的属性覆盖成热修复的类
        refHotFixObj(hotFixMap,properties,hotFixClassNameList);
        if(log.isInfoEnabled()){
            log.info(" hotfix complete ... ");
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext=applicationContext;
    }

    /**
     * 热修复的类有可能会有互相依赖的情况，这里对依赖的属性覆盖成热修复的类
     * @param hotFixMap 存放 实例化后的需要热修复的Bean映射关系，key=hotFixBeanName,value=hotFixBeanName对应的已经初始化的Bean
     * @param properties 抽象/接口类名 - 实现类名 的映射
     * @param hotFixClassNameList  需要热修复的类的类名集合
     */
    private void refHotFixObj(Map<String, Object> hotFixMap, Properties properties, Set<String> hotFixClassNameList) {
        for (Map.Entry<String, Object> entry : hotFixMap.entrySet()) {
            Object hotFixBean = entry.getValue();
            String hotFixBeanClassName = entry.getKey();
            //将 依赖Bean 的成员变量修改成引用 hotFixBean
            for (String dependentBeanName : refNewBeanHelper.findDependentBeanNames(hotFixBean)) {
                if(hotFixClassNameList.contains(dependentBeanName)){//属于热修复的类名
                    Object toRefBean = hotFixMap.get(dependentBeanName);
                    if(toRefBean==null && properties!=null){
                        String classNameImpl = properties.getProperty(dependentBeanName);
                        toRefBean=hotFixMap.get(classNameImpl);
                    }
                    if(toRefBean==null){
                        int lastDotIndex=dependentBeanName.lastIndexOf(".");
                        if(lastDotIndex!=-1){
                            String packageName = dependentBeanName.substring(0,lastDotIndex)+".impl.";
                            String clasNameImpl = dependentBeanName.substring(lastDotIndex + 1)+"Impl";
                            toRefBean=hotFixMap.get(packageName+clasNameImpl);
                        }
                    }
                    if(hotFixBean==RefNewBeanHelper.IGNORE_REF_NEW_BEAN_FLAG){
                        continue;
                    }
                    if(toRefBean==null) {
                        throw new HotFixException("found hotFixBeanClassName["+hotFixBeanClassName+"] depend on dependentBeanName ["+
                                dependentBeanName+"],but not found dependentBeanName instance");
                    }
                    refNewBeanHelper.refNewBean(hotFixBeanClassName,hotFixBean,dependentBeanName, toRefBean);
                }
            }
        }
    }

    /**
     * 获取 DefaultListableBeanFactory 对象
     */
    private DefaultListableBeanFactory getBeanFactory(){
        AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
        if(!(autowireCapableBeanFactory instanceof DefaultListableBeanFactory)){
            throw new HotFixException(" can not use hotFix cause not DefaultListableBeanFactory ");
        }
        return (DefaultListableBeanFactory) autowireCapableBeanFactory;
    }

    /**
     * 获取 热修复Bean对象生成器
     * @param beanFactory 当前上下文的Bean工厂
     */
    private HotFixBeanGenerator getHotFixBeanGenerator(DefaultListableBeanFactory beanFactory){
        if(hotFixBeanGenerator==null){
            hotFixBeanGenerator=new DefaultHotFixBeanGenerator(beanFactory,hotFixParams);
        }
        return hotFixBeanGenerator;
    }

    /**
     * 获取 抽象/接口类名 - 实现类名 的映射 properties文件
     */
    private Properties loadHofixClassMapProp(){
        String loadPath = hotFixParams.getLoadPath()+"hofix-class-map.properties";
        try {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(new File(loadPath)));
            Properties prop = new Properties();
            prop.load(new InputStreamReader(inputStream, "UTF-8"));
            return prop;
        } catch (IOException e) {
            if(log.isInfoEnabled()){
                log.info("load properties file [{}] fail ,so use default..,e = {}",e.getMessage());
            }
        }
        return null;
    }
}
