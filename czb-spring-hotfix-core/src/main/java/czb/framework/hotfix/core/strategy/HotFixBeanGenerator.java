package czb.framework.hotfix.core.strategy;

/**
 * 热修复Bean对象生成器
 * @author chenzhuobin
 */
public interface HotFixBeanGenerator {

    /**
     * 生成 hotFixBeanClass 的实例
     * @param hotFixBeanClass 热修复Bean类
     * @return hotFixBeanClass 的实例;返回null时，跳过对依赖原来Bean对象的Bean更新热修复Bean的操作。
     */
    Object generate(Class<?>  hotFixBeanClass);

    /**
     * 是否可以接受处理 hotFixBeanClass 去生成对应的实例
     * @param hotFixBeanClass 热修复Bean类
     */
    boolean canHandle(Class<?>  hotFixBeanClass);
}
