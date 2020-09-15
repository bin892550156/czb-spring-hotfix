package czb.framework.hotfix.core.strategy.impl;

import czb.framework.hotfix.core.exception.HotFixException;
import czb.framework.hotfix.core.strategy.HotFixBeanGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Spring MVC Controller 热修复Bean对象生成器
 * <p>通过覆盖 原来Controller 注册到 {@link RequestMappingInfoHandlerMapping} Bean 对象里的接口信息，使其调用到热修复的Controller中</p>
 * <p>该Controller对象支持：
 *  <ol>
 *   <li>原接口方法的热修复</li>
 *   <li>新增接口方法</li>
 *   <li>生成出来的Controller对象会经过 {@link DefaultListableBeanFactory#configureBean(Object, String)}方法填充属性。</li>
 *  </ol>
 * </p>
 * <p>注意实现：</p>
 */
public class ControllerHofFixBeanGenerator implements HotFixBeanGenerator {

    private Logger log= LoggerFactory.getLogger(CommonHotFixBeanGenerator.class);

    /**
     * 当前上下文Bean工厂
     */
    private DefaultListableBeanFactory beanFactory;
    /**
     * Spring MVC 管理 Controller 对象 的Bean对象
     */
    private RequestMappingInfoHandlerMapping requestMappingHandlerMapping;
    /**
     * requestMappingHandlerMapping#getMappingForMethod 方法对象。因为该方法是 protected ，所以需要通过反射方式调用。
     */
    private Method getMappingForMethod;

    /**
     * 新建一个 ControllerHofFixBeanGenerator 实例
     * @param beanFactory 当前上下文Bean工厂
     */
    public ControllerHofFixBeanGenerator(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        getHandlerMethodMapping();
        getMethodForGetMappingForMethod();
    }

    /**
     * 创建 热修复Controller 对象
     * <p>覆盖 原来Controller 注册到 {@link RequestMappingInfoHandlerMapping} Bean 对象里的接口信息，使其调用到热修复的Controller中</p>
     * @param hotFixBeanClass 热修复Bean类
     * @return 热修复Controller 对象
     */
    @Override
    public Object generate(Class<?> hotFixBeanClass) {

        String beanName = StringUtils.uncapitalize(hotFixBeanClass.getSimpleName());
        Object controller=null;
        if(beanFactory.containsBean(beanName)){
            controller=newController(hotFixBeanClass);
            beanFactory.configureBean(controller, beanName);
        }else{
            controller = beanFactory.createBean(hotFixBeanClass);
        }
        coverRegisterMapping(controller,hotFixBeanClass);
        return controller;
    }

    /**
     * hotFixBeanClass 必须有 {@link Controller},{@link RequestMapping} 注解
     * @param hotFixBeanClass 热修复Bean类
     */
    @Override
    public boolean canHandle(Class<?> hotFixBeanClass) {
        return AnnotatedElementUtils.hasAnnotation(hotFixBeanClass, Controller.class) ||
                AnnotatedElementUtils.hasAnnotation(hotFixBeanClass, RequestMapping.class);
    }

    /**
     * 使用 无参构造函数构建 hotFixBeanClass 的实例对象
     * @param hotFixBeanClass 热修复Bean类
     */
    private Object newController(Class<?> hotFixBeanClass){
        try {
            return hotFixBeanClass.newInstance();
        } catch (InstantiationException e) {
            if(log.isWarnEnabled()){
                log.warn(" className = {} newInstance fail ",hotFixBeanClass,e);
            }
        } catch (IllegalAccessException e) {
            if(log.isWarnEnabled()){
                log.warn(" className = {} default constructor method can not assert ",hotFixBeanClass,e);
            }
        }
        return null;
    }

    /**
     * 筛选handlerType合适作为接口的Method对象，构建 RequestMappingInfo 对象.然后覆盖 原Controller 已注册的接口信息。
     * @param controller 热修复Controller 对象
     * @param handlerType 热修复Bean类
     */
    private void coverRegisterMapping(Object controller, Class<?> handlerType){
        Class<?> userType = ClassUtils.getUserClass(handlerType);
        //筛选handlerType合适作为接口的Method对象
        Map<Method, RequestMappingInfo> methods = MethodIntrospector.selectMethods(userType,
                (MethodIntrospector.MetadataLookup<RequestMappingInfo>) method -> {
                    try {
                        //构建 RequestMappingInfo 对象
                        return invokeGetMappingForMethod(method, userType);
                    }
                    catch (Throwable ex) {
                        throw new IllegalStateException("Invalid mapping on handler class [" +
                                userType.getName() + "]: " + method, ex);
                    }
                });
        //覆盖 原Controller 已注册的接口信息。
        methods.forEach((method, mapping) -> {
            Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
            invokeRegisterHandlerMethod(controller, invocableMethod, mapping);
        });
    }

    /**
     * 覆盖 原Controller 已注册的接口信息。
     * @param controller 热修复 Controller 对象
     * @param invocableMethod 要注册的可作为接口调用的合适Method对象
     * @param mapping 根据 controller,invocableMethod构建出来的RequestMappingInfo对象
     */
    private void invokeRegisterHandlerMethod(Object controller, Method invocableMethod, RequestMappingInfo mapping) {
        requestMappingHandlerMapping.unregisterMapping(mapping);
        requestMappingHandlerMapping.registerMapping(mapping,controller,invocableMethod);
    }

    /**
     * 构建 RequestMappingInfo 对象
     * <p>反射调用 requestMappingHandlerMapping#getMappingForMethod 方法</p>
     * @param method 热修复 Controller 对象合适的Method对象【requestMappingHandlerMapping#getMappingForMethod所需参数值】
     * @param handlerType 热修复Bean类 【requestMappingHandlerMapping#getMappingForMethod所需参数值】
     * @return requestMappingHandlerMapping#getMappingForMethod 的返回结果
     */
    private RequestMappingInfo invokeGetMappingForMethod(Method method, Class<?> handlerType){
        try {
            RequestMappingInfo requestMappingInfo= (RequestMappingInfo) getMappingForMethod.invoke(requestMappingHandlerMapping,method,handlerType);
            return requestMappingInfo;
        }  catch (IllegalAccessException e) {
            throw new HotFixException(handlerType.getName()+" invoke '"+method.getName()+"' fail,cause no access",e);
        } catch (InvocationTargetException e) {
            throw new HotFixException(handlerType.getName()+" invoke '"+method.getName()+"' fail,cause throw exception",e.getTargetException());
        }
    }

    private void getHandlerMethodMapping(){
        try{
            Object bean= beanFactory.getBean("requestMappingHandlerMapping");
            if(!(bean instanceof RequestMappingInfoHandlerMapping)){
                throw new HotFixException("'requestMappingHandlerMapping' bean can not cast to "+AbstractHandlerMethodMapping.class.getName());
            }
            requestMappingHandlerMapping= (RequestMappingInfoHandlerMapping) bean;
        }catch (NoSuchBeanDefinitionException e){
            throw new HotFixException("no found 'requestMappingHandlerMapping' bean in BeanFactory ",e);
        }
    }

    private Method getMethodForGetMappingForMethod(){
        Method[] declaredMethods = requestMappingHandlerMapping.getClass().getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            if("getMappingForMethod".equals(declaredMethod.getName())){
                getMappingForMethod=declaredMethod;
                break;
            }
        }
        if(getMappingForMethod==null){
            throw new HotFixException(requestMappingHandlerMapping.getClass().getName()+"no found 'getMappingForMethod' method ");
        }
        getMappingForMethod.setAccessible(true);
        return getMappingForMethod;
    }
}
