package czb.framework.hotfix.core.strategy.impl;

import czb.framework.hotfix.core.strategy.HotFixBeanGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.StringUtils;

/**
 * 通用热修复 Bean 对象生成器
 * <p>构建热修复 Bean 对象，该对象会经过 {@link DefaultListableBeanFactory#configureBean(Object, String)}方法填充属性</p>
 * @author chenzhuobin
 */
public class CommonHotFixBeanGenerator implements HotFixBeanGenerator {

    private Logger log= LoggerFactory.getLogger(CommonHotFixBeanGenerator.class);
    /**
     * 当前应用上下文的Bean工厂
     */
    private DefaultListableBeanFactory beanFactory;

    /**
     * 新建一个 CommonHotFixBeanGenerator 对象
     * @param beanFactory 当前应用上下文的Bean工厂
     */
    public CommonHotFixBeanGenerator(DefaultListableBeanFactory beanFactory) {
        this.beanFactory=beanFactory;
    }

    @Override
    public Object generate(Class<?> hotFixBeanClass) {
        if(hotFixBeanClass.isInterface()||hotFixBeanClass.isAnnotation()||hotFixBeanClass.isEnum()){
            return null;
        }
        Object hotFixBean= null;
        try {
            String beanName = StringUtils.uncapitalize(hotFixBeanClass.getSimpleName());
            if(beanFactory.containsBean(beanName)){
                hotFixBean = hotFixBeanClass.newInstance();
                beanFactory.configureBean(hotFixBean, beanName);
            }else{
                hotFixBean = beanFactory.createBean(hotFixBeanClass);
            }
        } catch (IllegalAccessException e) {
            if(log.isWarnEnabled()){
                log.warn(" className = {} default constructor method can not assert ",hotFixBeanClass,e);
            }
        } catch (InstantiationException e) {
            if(log.isWarnEnabled()){
                log.warn(" className = {} newInstance fail ",hotFixBeanClass,e);
            }
        }
        return hotFixBean;
    }

    @Override
    public boolean canHandle(Class<?> hotFixBeanClass) {
        return true;
    }
}
