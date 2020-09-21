package czb.framework.hotfix.core.strategy.impl;

import czb.framework.hotfix.core.config.HotFixProperties;
import czb.framework.hotfix.core.strategy.HotFixBeanGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认的 HotFixBeanGenerator
 * <p>用于匹配接收给定 hotFixBeanClass 的 HotFixBeanGenerator 进行相关业务处理</p>
 * @author chenzhuobin
 */
public class DefaultHotFixBeanGenerator implements HotFixBeanGenerator {



    /**
     * 当前应用上下文的Bean工厂
     */
    private DefaultListableBeanFactory beanFactory;

    /**
     * HotFixBeanGenerator 集合，必须要保证元素顺序，因为只使用第一个匹配到的HotFixBeanGenerator进行生成热修复的Bean对象。
     */
    private List<HotFixBeanGenerator> hotFixBeanGenerators;


    /**
     * 新建一个 MybatisHotFixBeanGenerator 对象
     * @param beanFactory 当前应用上下文的Bean工厂
     */
    public DefaultHotFixBeanGenerator(DefaultListableBeanFactory beanFactory, HotFixProperties hotFixProperties) {
        this.beanFactory=beanFactory;
        hotFixBeanGenerators=new ArrayList<>();
        loadHotFixBeanGenerators(beanFactory, hotFixProperties);
    }


    /**
     * 找到 可接收处理 hotFixbeanClass 的 HotFixBeanGenerator 进行生成 热修复的Bean对象，如果找不到就返回 null
     * @param hotFixBeanClass 热修复Bean类
     * @return hotFixBeanClass 的实例;返回null时，跳过对依赖原来Bean对象的Bean更新热修复Bean的操作
     */
    @Override
    public Object generate(Class<?> hotFixBeanClass) {
        for (HotFixBeanGenerator hotFixBeanGenerator : hotFixBeanGenerators) {
            if(hotFixBeanGenerator.canHandle(hotFixBeanClass)){
                return hotFixBeanGenerator.generate(hotFixBeanClass);
            }
        }
        return null;
    }

    /**
     * 是否有 可接收处理 hotFixbeanClass 的 HotFixBeanGenerator
     * @param hotFixBeanClass 热修复Bean类
     * @return true，表示找到了；否则返回false
     */
    @Override
    public boolean canHandle(Class<?> hotFixBeanClass) {
        for (HotFixBeanGenerator hotFixBeanGenerator : hotFixBeanGenerators) {
            if(hotFixBeanGenerator.canHandle(hotFixBeanClass)){
               return true;
            }
        }
        return false;
    }

    /**
     * 加载默认的 热修复Bean对象生成器
     * @param beanFactory bean工厂
     * @param hotFixProperties 热修复参数配置
     */
    private void loadHotFixBeanGenerators(DefaultListableBeanFactory beanFactory, HotFixProperties hotFixProperties){
        hotFixBeanGenerators.add(new MybatisHotFixBeanGenerator(beanFactory, hotFixProperties));
        hotFixBeanGenerators.add(new ControllerHofFixBeanGenerator(beanFactory));
        hotFixBeanGenerators.add(new CommonHotFixBeanGenerator(beanFactory));
    }

    /**
     * 添加
     * @param generator
     */
    private void addHotFixBeanGenerator(HotFixBeanGenerator generator){
        hotFixBeanGenerators.add(generator);
    }
}
