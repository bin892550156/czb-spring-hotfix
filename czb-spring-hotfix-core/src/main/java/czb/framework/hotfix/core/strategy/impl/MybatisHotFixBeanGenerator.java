package czb.framework.hotfix.core.strategy.impl;

import com.baomidou.mybatisplus.core.MybatisMapperAnnotationBuilder;
import czb.framework.hotfix.core.config.HotFixParams;
import czb.framework.hotfix.core.exception.HotFixException;
import czb.framework.hotfix.core.helper.RefNewBeanHelper;
import czb.framework.hotfix.core.strategy.HotFixBeanGenerator;
import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mybatis Mapper对象 热修复Bean对象生成器
 * <p>通过反射方式 {@link Configuration} 清除原 Mapper 注册的信息</p>
 * <p>并不会构建热修复Mapper对象，因为原 Mapper 对象是个代理对象，当调用Mapper方法，代理会从 {@link Configuration} 中获取对应的
 * SQL 脚本去执行。所以执行要解析热修复 Mapper 类的原信息然后覆盖已注册到 {@link Configuration} 里的记录以达到热修复效果。</p>
 * <p>热修复后的Mapper支持：
 *  <ol>
 *   <li>支持原Mapper层热修复</li>
 *   <li>支持Mapper.xml层热修复</li>
 *   <li>【不支持新增Mapper层】</li>
 *   <li>兼容 Mybatis-Plus 框架</li>
 *  </ol>
 * </p>
 * <p>注意实现：
 *  <ol>
 *   <li>不支持新增接口方法，因为依赖的Bean对象调用的还是原Mapper对象，即使新对象增
 *   加了新接口方法，也没法触发到新方法上。</li>
 *   <li>热修复的Mapper.xml必须写在热修复 Mapper 接口类的所在包下</li>
 *   <li>由于该类主要通过反射覆盖 {@link Configuration} 中字段，所以再重写Configuration时，需要
 *   保证对应的属性名没有缺失，且属性类型一致。</li>
 *   <li>不会生成热修复 Mapper 对象，仅解析热修复 Mapper 类的原信息然后覆盖已注册到 {@link Configuration}
 *   里的记录以达到热修复效果</li>
 *   <li>仅可以热修复 mapper 所定义的方法，忽略 mapper 的继承关系。</li>
 *  </ol>
 * </p>
 * @author chenzhuobin
 */
public class MybatisHotFixBeanGenerator implements HotFixBeanGenerator {


    private Logger log= LoggerFactory.getLogger(MybatisHotFixBeanGenerator.class);

    /**
     * 当前应用上下文的Bean工厂
     */
    private DefaultListableBeanFactory beanFactory;

    /**
     * 热修复参数配置
     */
    private HotFixParams hotFixParams;

    /**
     * MyBatis的主类名，用于判断是否有加载Mybatis的依赖
     */
    private final static String MYBATIS_MAIN_CLASS="org.apache.ibatis.session.Configuration";

    /**
     * 是否有依赖 Mybatis-Plus 框架
     */
    private boolean isDependentMyBatis=false;

    /**
     * 新建一个 MybatisHotFixBeanGenerator 对象
     * @param beanFactory 当前应用上下文的Bean工厂
     */
    public MybatisHotFixBeanGenerator(DefaultListableBeanFactory beanFactory, HotFixParams hotFixParams) {
        this.beanFactory=beanFactory;
        this.hotFixParams=hotFixParams;
        try {
            getClass().getClassLoader().loadClass(MYBATIS_MAIN_CLASS);
            isDependentMyBatis=true;
        } catch (ClassNotFoundException e) {
            //ignore
        }
    }

    /**
     * 不会构建热修复Mapper对象，只是解析热修复 Mapper 类的原信息然后覆盖已注册到 {@link Configuration} 里的记录即可达到
     * 热修复的目的
     * @param hotFixBeanClass 热修复Bean类
     * @return null,因为不需要生成热修复 Mapper 对象，所以不需要对依赖原来Bean对象的Bean更新热修复Bean的操作。
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object generate(Class<?> hotFixBeanClass) {
        DefaultSqlSessionFactory sqlSessionFactory = (DefaultSqlSessionFactory) beanFactory.getBean("sqlSessionFactory");
        Configuration configuration = sqlSessionFactory.getConfiguration();
        //删除 hotFixBeanClass 在 configuration 中的元数据信息
        removeMybatisMedata(configuration,hotFixBeanClass);
        //加载 Mappper.xml
        loadXmlMapper(hotFixBeanClass,configuration);
        //加载接口上的方法
        if(isHaveMybatisPlusDependent()){
            new MybatisMapperAnnotationBuilder(configuration, hotFixBeanClass).parse();
        }else{
            new MapperAnnotationBuilder(configuration,hotFixBeanClass).parse();
        }
        return RefNewBeanHelper.IGNORE_REF_NEW_BEAN_FLAG;
    }

    @Override
    public boolean canHandle(Class<?> hotFixBeanClass) {
        if(!isDependentMyBatis) return false;
        return hotFixBeanClass.getAnnotation(Repository.class)!=null && hotFixBeanClass.isInterface();
    }

    /**
     * 删除 hotFixBeanClass 在 configuration 中的元数据信息
     * @param configuration Mybatis 配置中心
     * @param hotFixBeanClass 热修复Bean类
     */
    private void removeMybatisMedata(Configuration configuration, Class<?> hotFixBeanClass){
        MapperRegistry mapperRegistry = configuration.getMapperRegistry();
        //删除hotFixBean原来的缓存
        Map<Class<?>,?> registryMapper = getRegistryMapper(mapperRegistry);
        Object originalMapperProxyFactory = getMapperProxyFactory(registryMapper,hotFixBeanClass.getName());
        //清除Mapper代理对象的缓存
        if(originalMapperProxyFactory==null) {
            return ;
        }
        Map methodCache = getMethodCache(originalMapperProxyFactory);
        methodCache.clear();
        //删除configuration#loadedResources的缓存
        Set loadResource = getLoadResource(configuration);
        loadResource.remove(hotFixBeanClass.toString());
        loadResource.remove(generateXmlMapper(hotFixBeanClass));
        // mapper 中的 key
        List<String> keyPreFixs=getKeyPrefixs(hotFixBeanClass);
        //删除 configuration # mappedStatements 的 originalClass 缓存
        Map<String,?> mappedStatements = getMappedStatements(configuration);
        removeMappedStatements(mappedStatements,keyPreFixs);
        //删除 configuration # caches 的 originalClass 缓存
        Map<String,?> caches = getCaches(configuration);
        removeKey(caches,hotFixBeanClass.getName());
        //删除 configuration # resultMaps 的 originalClass 缓存
        Map<String,?> resultMaps = getResultMaps(configuration);
        removeKey(resultMaps,hotFixBeanClass.getName());
        //删除 configuration # parameterMap 的 originalClass 缓存
        Map<String,?> parameterMap = getParameterMap(configuration);
        removeKey(parameterMap,hotFixBeanClass.getName()+".");
        //删除 configuration # keyGenerator 的 originalClass 缓存
        Map<String, ?> keyGenerator = getKeyGenerator(configuration);
        removeSelectKey(keyGenerator,keyPreFixs);
    }

    /**
     * 是否有依赖 Mybatis-Plus 框架
     */
    private boolean isHaveMybatisPlusDependent(){
        boolean haveMybatisPlusDependent=false;
        try {
            getClass().getClassLoader().loadClass("org.apache.ibatis.builder.annotation.MapperAnnotationBuilder");
            haveMybatisPlusDependent=true;
        } catch (ClassNotFoundException e) {
            //ignore
        }
        return haveMybatisPlusDependent;
    }

    /**
     * 加载 mapper.xml ,并将解析的数据注册到 {@link Configuration} 中
     * <p>读取在 hotFixBeanClass 包里的 mapper.xml </p>
     * @param hotFixBeanClass 热修复Bean类
     * @param configuration Mybatis 配置中心
     */
    private void loadXmlMapper(Class<?> hotFixBeanClass,Configuration configuration){
        String xmlResource= generateXmlMapper(hotFixBeanClass);
        try {
            FileInputStream fin=new FileInputStream(new File(hotFixParams.getLoadPath()+xmlResource));
            XMLMapperBuilder xmlParser = new XMLMapperBuilder(fin, configuration, xmlResource, configuration.getSqlFragments(), hotFixBeanClass.getName());
            xmlParser.parse();
        } catch (FileNotFoundException e) {
            //ignore
        }
    }

    /**
     * 构建 hotFixBeanClass 对应的 Mapper.xml路径
     * @param hotFixBeanClass 热修复Bean类
     */
    private String generateXmlMapper(Class<?> hotFixBeanClass){
        return hotFixBeanClass.getName().replace('.', File.separatorChar) + ".xml";
    }

    /**
     * 解析 Mapper 接口的自己定义的方法，构建 Map.key 前缀【类名.方法名】
     * <p>>仅过滤出 热修复Bean类 所定义的方法名，忽略 mapper 的继承关系。</p
     * @param hotFixBeanClass 热修复Bean类
     */
    public List<String> getKeyPrefixs(Class<?> hotFixBeanClass){
        String hotFixBeanClassName=hotFixBeanClass.getName();
        Method[] methods = hotFixBeanClass.getMethods();
        List<String> keyPreFixs=new ArrayList<>(methods.length);
        for (Method method : methods) {
            if(method.getDeclaringClass()==hotFixBeanClass){
                keyPreFixs.add(hotFixBeanClassName+"."+method.getName());
            }
        }
        return keyPreFixs;
    }

    //—————————————————————————— 删除原 Mapper 类注册到 Configuration 对象的相关缓存数据————————————————

    /**
     * 获取 原Mapper 对应的 MapperProxyFactory 对象
     * <p>MapperProxyFactory 对象用于生成 Mapper接口的代理对象的工厂类</p>
     * @param registryMapper 已注册的Mapper，key=原Mapper接口类，MapperProxyFactory 对象
     * @param className 热修复Bean类名
     */
    private Object getMapperProxyFactory(Map<Class<?>,?> registryMapper,String className){
        for (Map.Entry<Class<?>, ?> entry : registryMapper.entrySet()) {
            if(entry.getKey().getName().equals(className)){
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 删除 在 map 中以 keyPreFix 作为前缀的元素
     * @param map configuration # caches,configuration # resultMaps,configuration # parameterMap
     * @param keyPreFix key前缀
     */
    private void removeKey(Map<String,?> map, String keyPreFix){
        map.entrySet().removeIf(entry->entry.getKey().startsWith(keyPreFix));
    }

    /**
     * 删除 在 mappedStatements 中以 keyPreFixs 作为前缀的元素
     * @param mappedStatements configuration # mappedStatements
     * @param keyPreFixs key前缀集合
     */
    private void removeMappedStatements(Map<String,?> mappedStatements,List<String> keyPreFixs){
        mappedStatements.entrySet().removeIf(entry -> {
            String key=entry.getKey();
            for (String keyPriFix : keyPreFixs) {
                if(key.startsWith(keyPriFix)){
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * 删除 在 mappedStatements 中以 keyPreFixs 作为前缀的元素
     * @param keyGenerator configuration # keyGenerator
     * @param keyPreFixs key前缀集合
     */
    private void removeSelectKey(Map<String,?> keyGenerator, List<String> keyPreFixs){
        keyGenerator.entrySet().removeIf(entry -> {
            String key=entry.getKey();
            for (String keyPriFix : keyPreFixs) {
                if(key.startsWith(keyPriFix+"!selectKey")){
                    return true;
                }
            }
            return false;
        });
    }

    //——————————————————————————————反射获取 Mybatis 中的缓存属性，如 Configuration的相关属性————————————————————

    /**
     * 反射获取 Configuration # MapperRegistry # knownMappers 属性
     * @param mapperRegistry Configuration # MapperRegistry对象
     */
    @SuppressWarnings("rawtypes")
    protected Map<Class<?>,?> getRegistryMapper(MapperRegistry mapperRegistry){
        return (Map) getFieldValue(mapperRegistry.getClass(),mapperRegistry,"knownMappers",Map.class);
    }

    /**
     * 反射获取 Configuration # MapperRegistry # knownMappers 属性中 Mapper 类 对应的 MapperProxyFactory 对象的
     * methodCache 属性
     * @param mapperProxyFactory Configuration # MapperRegistry # knownMappers 属性中 Mapper 类 对应的 MapperProxyFactory 对象
     */
    @SuppressWarnings("rawtypes")
    protected Map getMethodCache(Object mapperProxyFactory){
        log.info(mapperProxyFactory.getClass().getName());
        return (Map) getFieldValue(mapperProxyFactory.getClass(),mapperProxyFactory,"methodCache",Map.class);
    }

    /**
     * 反射获取 Configuration # loadedResources 属性
     * @param configuartion {@link Configuration} 对象
     */
    @SuppressWarnings("rawtypes")
    protected Set getLoadResource(Object configuartion){
        return (Set) getConfigurateFieldValue(configuartion,"loadedResources",Set.class);
    }

    /**
     * 反射获取 Configuration # mappedStatements 属性
     * @param configuartion {@link Configuration} 对象
     */
    protected Map<String,?> getMappedStatements(Object configuartion){
        return (Map) getConfigurateFieldValue(configuartion,"mappedStatements",Map.class);
    }

    /**
     * 反射获取 Configuration # caches 属性
     * @param configuartion {@link Configuration} 对象
     */
    protected Map<String,?> getCaches(Object configuartion){
        return (Map) getConfigurateFieldValue(configuartion,"caches",Map.class);
    }

    /**
     * 反射获取 Configuration # keyGenerators 属性
     * @param configuartion {@link Configuration} 对象
     */
    protected Map<String,?> getKeyGenerators(Object configuartion){
        return (Map) getConfigurateFieldValue(configuartion,"keyGenerators",Map.class);
    }

    /**
     * 反射获取 Configuration # resultMaps 属性
     * @param configuartion {@link Configuration} 对象
     */
    protected Map<String,?> getResultMaps(Object configuartion){
        return (Map<String,?>) getConfigurateFieldValue(configuartion,"resultMaps",Map.class);
    }

    /**
     * 反射获取 Configuration # parameterMaps 属性
     * @param configuartion {@link Configuration} 对象
     */
    protected Map<String,?> getParameterMap(Object configuartion){
        return (Map<String,?>) getConfigurateFieldValue(configuartion,"parameterMaps",Map.class);
    }

    /**
     * 反射获取 Configuration # parameterMaps 属性
     * @param configuartion {@link Configuration} 对象
     */
    protected Map<String,?> getSqlFragments(Object configuartion){
        return (Map<String,?>) getConfigurateFieldValue(configuartion,"sqlFragments",Map.class);
    }


    /**
     * 反射获取 Configuration # keyGenerators 属性
     * @param configuartion {@link Configuration} 对象
     */
    protected Map<String,?> getKeyGenerator(Object configuartion){
        return (Map<String,?>) getConfigurateFieldValue(configuartion,"keyGenerators",Map.class);
    }

    /**
     * 获取 configuartion 属性中 fieldName 的属性，并进行 matchType 类型匹配
     * @param configuartion configuartion 对象
     * @param fieldName 属性名
     * @param matchType 属性要匹配的类型，匹配失败会抛出 {@link HotFixException}
     */
    protected Object getConfigurateFieldValue(Object configuartion,String fieldName,Class<?> matchType){
        Class<?> configuartionCls=configuartion.getClass();
        Object fieldValue =null;
        try{
            fieldValue=getFieldValue(configuartionCls, configuartion, fieldName, matchType);
            return fieldValue;
        }catch (HotFixException e){
            if(!(e.getCause() instanceof NoSuchFieldException)){
                throw e;
            }
        }
        if(configuartionCls.getSuperclass()==Configuration.class){
            configuartionCls=Configuration.class;
        }
        return getFieldValue(configuartionCls, configuartion, fieldName, matchType);
    }

    /**
     * 从 cls中获取fieldName对应的Field对象，然后通过 obj 获取 fieldName 的属性，最后进行 matchType 类型匹配
     * @param cls 要被获取 fieldName Field对象的cls
     * @param obj 要被获取 fieldName 属性值的 Object 对象
     * @param fieldName 属性名
     * @param matchType 属性要匹配的类型，匹配失败会抛出 {@link HotFixException}
     */
    protected Object getFieldValue(Class<?> cls,Object obj,String fieldName,Class<?> matchType){
        try {
            Field loadedResourcesField=cls.getDeclaredField(fieldName);
            loadedResourcesField.setAccessible(true);
            Object o = loadedResourcesField.get(obj);
            if(o!=null && matchType==o.getClass()){
                throw new HotFixException(" the '"+fieldName+"' field of "+cls.getName()+" is not "+Set.class.getName());
            }
            return  o;
        } catch (NoSuchFieldException e) {
            throw new HotFixException(cls.getName()+" no found '"+fieldName+"' field",e);
        } catch (IllegalAccessException e) {
            throw new HotFixException(cls.getName()+" get '"+fieldName+"' field value fail",e);
        }
    }

}
