# 不需要重启服务的热修复框架
这个一个基于Spring的热修复框架，该框架不需要重启服务，是一个针对特殊生产环境的热修复框架。
与dev-tool不一样，该框架不需要刷新Spring容器，也不监听class文件的变化，也不需要覆盖原有的class文件。

# 原理
本框架都建立在破坏双亲委派机制``czb.framework.hotfix.core.classloader.HotFixClassLoader``去加载热修复class文件。
## 基本原则
**由于 JAVA 的安全机制认为 不同的ClassLoader之间就算加载了同一个类，也不可以认为这个类是同一个类，这就意味着不同ClassCloader
加载的同一个类是不可以直接赋值或者强转**
## 一 针对 Controller 层
Spring mvc 其实是将 Controller 对象进行解析出【url-Method对象】注册到 
``org.springframework.web.servlet.mvc.method.Class RequestMappingInfoHandlerMapping`` 中，当有
接口过来的时候，匹配接口的url，从而找到对应的Method对象，从而进行反射调用。基于这个原理，我可以
修改 ``RequestMappingInfoHandlerMapping`` 中的注册表内容，就能调用到热修复Controller对象。
## 二 针对 Mapping 层
Mybatis 将解析到 Mapper层的接口注解信息，以及Mapper.xml的配置信息都注册在 ``org.apache.ibatis.session.Configuration``
中【Configuration 就像 Mybatis 的注册中心，所有配置信息都放在这里包括Mapper.xml,Mapper接口】，而我们调用的Mapper层对象，其
是Mybatis对Mapper层构建出来的代理对象，而每次调用Mapper层对象方法其实都是从Configuration获取对应的 ``MappedStatement`` 对象
以实现对应的业务逻辑。基于这个原理，我可以更新关于热修复Mapper层在``Configuration``的注册信息，以达到热修复的目的。
## 三 针对其他普通的java类
由于 JAVA 的安全机制认为 不同的ClassLoader之间就算加载了同一个类，也不可以认为这个类是同一个类，这就意味着 不同ClassCloader加载
的同一个类是不可以直接赋值或者强转。但是我们可以父子级继承方式去绕过这个安全机制。比如 Controller 层调用 Service层时，如果Controller
直接调用 Service层的实现类，那就意味着该Service层没法进行热修复,但是如果 Controller 时调的是 Service层的接口，那Service层的实现类
就可以实现热修复。【还不是很理解的读者，请查看 czb-spring-hotfix-core 模块的
 `czb.framework.hotfix.core.HotFixClassLoaderTest # test_HotFixClassLoader()` 测试用例】

## 功能
### Controller层
1. 支持原Controller层热修复
2. 支持新增Controller层
3. 生成出来的Controller对象会经过 ``DefaultListableBeanFactory.configureBean(Object, String)``
    方法填充属性

### Service层
1. 支持新增非重新方法。
2. 支持新增Service层
3. 支持原Service层实现类代码热修复
4.【不支持原Service层接口热修复】
5. 生成出来的Service对象会经过 ``DefaultListableBeanFactory.configureBean(Object, String)``
       方法填充属性

### Mybatis Mapper层
1. 支持原Mapper层热修复
2. 支持Mapper.xml层热修复
3. 【不支持新增Mapper层】
4. 兼容 Mybatis-Plus 框架

### 实体类
1. 支持新增实体类
2. 【不支持原实体类的热修复，但支持新增子类以实现热修复】
5. 生成出来的对象会经过 ``DefaultListableBeanFactory.configureBean(Object, String)``
       方法填充属性

# 开始入手
主要看 czb-spring-hotfix-demo 样例
## 配置
热修复的类文件放在 czb-spring-hotfix-demo 的 hotfix 文件夹下。
其中 hofix-class-map.properties 是一个 【接口/抽象类名 - 热修复实现类名】 的 properties 文件，
用于 在热修复类互相依赖的情况下，对依赖属性【即Class # Field】覆盖成热修复类时找出对应的热修复实现类。如果该文件
不存在时，遇到这种情况，默认是获取依赖属性class类名修改成常规实现类名获取
【如：czb.framework.hotfix.demo.service.UserService 默认改成 czb.framework.hotfix.demo.service.impl.UserServiceImpl】
```java
@Configuration
public class HotFixConfig {

    @Bean
    public HotFix hotFix(){
        HotFixParams hotFixProperties=new HotFixParams();
        //本地文件加载地址
        hotFixProperties.setLoadPath("E:\\Project\\Java\\OpenSource\\czb-spring-hotfix\\czb-spring-hotfix-demo\\hotfix");
        //基础包名
        hotFixProperties.setBasePackage("czb.framework.hotfix.demo");
        //需要加载到AppClassLoader【父级ClassLoader】的包名
        hotFixProperties.setShouldLoadInAppClassLoaderPackage(Arrays.asList(
                "czb.framework.hotfix.demo.entity",
                "czb.framework.hotfix.demo.vo.resq"));
        return new HotFix(hotFixProperties);
    }
}
```
## 使用
```java
@RestController
public class HotFixController {

    @Autowired
    HotFix hotFix;

    @PutMapping("/hotfix")
    public ApiResult hotfix(){
        //启动热修复
        hotFix.exec();
        return ApiResult.success();
    }
}
```

#常见问题
## 热修复的类支持新增方法吗？
支持，但是要保证不要破坏基本原则的前提下。比如 Controller 调用 Service 层接口方法，但是热修复的时候想增加Service层接口方法让
Controller调用，这是不支持的。但是想在Service层接口实现类上增加方法，修改方法，增加变量等等的内部操作是完全支持的，你甚至可以
通过热修复新增Controller，Service层。
## 为啥Mapper层不支持热修复的新增？
这个可以有，但是由于有点麻烦，后面真的有人用时，我再逐步完善。
## Mapper层支持新增接口方法吗？
其实支持的，因为mybatis并不是直接调用接口方法，而是通过代理类进行调用到指定的 Configuration 管理的 MappedStatement 对象，所以
新增的接口方法可以注册到 Configuration 中的，但是由于 基本原则 原因，导致 调用Mapper层对象中并没有这个新增的热修复方法从而报错，
你可以通过 SqlSessionTemplate 指定该新增的热修复方法的 MappedStatement Id 从而调用到新MappedStatement对象中。还不是很理解的
读者，请看博客：[https://blog.csdn.net/qq_30321211/article/details/108605484](https://blog.csdn.net/qq_30321211/article/details/108605484)
## 该框架为啥没有安全方面的控制呢？
因为本框架所实现的功能本来就是不安全的，我并不打算考虑安全的问题，安全问题交给调用者吧，所以调用者要考虑到使用该框架时会出现的一系列
安全问题。

# 个人期望
希望这个框架会受到更多人的关注吧。如果对该框架还有什么不理解的地方或者什么建议的地方，可以加我的微信：892550156
博客：[https://mp.csdn.net/console/article](https://mp.csdn.net/console/article)