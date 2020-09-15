package czb.framework.hotfix.core.classloader;

import czb.framework.hotfix.core.config.HotFixParams;
import czb.framework.hotfix.core.exception.HotFixException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 热修复ClassLoader
 *
 * <p>不可以重写 loadClass,因为java默认加载对象其实使用通过当前类的类加载器 {@link ClassLoader#loadClass(String)}进行获取目标Class对象，
 * 因为HotFixClassLoader是破坏了双亲委派机制的类加载器，所以如果出现 需要热修复的两个类有依赖关系时，依赖的类中的被依赖类的field的
 * 类对象与原类对象不是同一个，导致Spring的自动装配功能在匹配类型时因classLoader不是同一个而失败。
 * </p>
 * @author chenzhuobin
 */
public class HotFixClassLoader extends ClassLoader{

    /**
     * 热修复参数配置
     */
    private HotFixParams hotFixParams;

    /**
     * ClassLoader映射【key=热修复类名,value= InnerHotFixClassLoader 对象】
     * <p><b>为什么要每个热修复类对应一个 InnerHotFixClassLoader 对象呢？</b></p>
     * <p>因为如果将所有热修复类都放在同一个 ClassLoader 上时，如果热修复类中有引用热修复类
     * 的字段时会导致该字段类型与 父类加载器的类 不匹配，导致Spring的字段装配或者需要设置 父类加载器的类 的对象
     * 到该字段时报转换错误。最典型的情况就是Mybatis的Mapper接口。</p>
     */
    Map<String,InnerHotFixClassLoader> classLoaderMap=new HashMap<>();

    /**
     * 父级类加载器
     */
    private ClassLoader parent;

    /**
     * 新建一个 {@link HotFixClassLoader} 对象
     * @param classLoader 父级类记载器
     * @param hotFixParams 热修复参数配置
     */
    public HotFixClassLoader(ClassLoader classLoader, HotFixParams hotFixParams ) {
        super(classLoader);
        parent=classLoader;
        File classLocalPath = new File(hotFixParams.getLoadPath());
        this.hotFixParams = hotFixParams;
        recursionClassFile(classLocalPath);
    }

    /**
     * 遍历项目的class文件
     */
    private void recursionClassFile(File classLocalPath) {
        if (classLocalPath.isDirectory()) {
            File[] files = classLocalPath.listFiles();
            if(files==null) return;
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                recursionClassFile(file);
            }
        } else if (classLocalPath.isFile() && classLocalPath.getName().endsWith(".class") ) {
            InnerHotFixClassLoader classLoader=new InnerHotFixClassLoader(hotFixParams,classLocalPath,parent);
            classLoaderMap.put(classLoader.getClassName(),classLoader);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        ClassLoader classLoader = classLoaderMap.get(name);
        if (classLoader == null) {
            classLoader=getParent();
        }
        Class<?> cls = classLoader.loadClass(name);
        if (cls == null) {
            throw new ClassNotFoundException(name);
        }
        if (resolve) {
            resolveClass(cls);
        }
        return cls;
    }

    /**
     * 获取 ClassLoader映射
     */
    public Map<String, InnerHotFixClassLoader> getClassLoaderMap() {
        return classLoaderMap;
    }


    /**
     * 内部 热修复专业类加载器【一个热修复类对应一个 InnerHotFixClassLoader 】
     *
     * @author chenzhuobin
     */
    private static class InnerHotFixClassLoader extends ClassLoader{

        /**
         * 热修复参数配置
         */
        private HotFixParams hotFixParams;

        /**
         * 类名
         */
        private String className;

        /**
         * 父级类加载器
         */
        private ClassLoader parent;
        /**
         * 新建一个 InnerHotFixClassLoader 对象
         * @param hotFixParams 热修复参数配置
         * @param classPathFile 热修复类文件
         * @param parent 父级类加载器
         */
        public InnerHotFixClassLoader(HotFixParams hotFixParams, File classPathFile, ClassLoader parent){
            super(parent);
            this.parent=parent;
            this.hotFixParams=hotFixParams;
            getClassData(classPathFile);
        }
        /**
         * 获取热修复类文件的类数据
         */
        private void getClassData(File classPathFile) {
            try(InputStream fin = new FileInputStream(classPathFile)){
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int bufferSize = 4096;
                byte[] buffer = new byte[bufferSize];
                int byteNumRead = 0;
                while ((byteNumRead = fin.read(buffer)) != -1) {
                    bos.write(buffer, 0, byteNumRead);
                }
                byte[] classBytes = bos.toByteArray();
                className = getClassName(classPathFile);
                //是否应该调用
                if(shouldLoadInAppClassLoader(className)){
                    invokeParent_defineClass(className,classBytes,0,classBytes.length);
                }else{
                    defineClass(className, classBytes, 0, classBytes.length);
                }
            } catch (IOException e) {
               throw new HotFixException(" load class File "+classPathFile.getAbsolutePath()+" fail",e);
            } catch (NoClassDefFoundError e){
                e.printStackTrace();
                //有可能是 新热修复类，所以尝试使用父级ClassLoader进行加载，只有通过父级ClassLoader加载才能
                // 使得 该类加载器找到该热修复类。
                getClassDataToParent(hotFixParams.getLoadPath()+e.getMessage()+".class");
                getClassData(classPathFile);
            }
        }

        /**
         * 是否应该调用
         * @param className 类名
         */
        private boolean shouldLoadInAppClassLoader(String className){
            List<String> shouldLoadInAppClassLoaderPackages = hotFixParams.getShouldLoadInAppClassLoaderPackage();
            if(shouldLoadInAppClassLoaderPackages==null) return false;
            int lastDotIndex=className.lastIndexOf(".");
            String packageName = className.substring(0, lastDotIndex);
            return shouldLoadInAppClassLoaderPackages.stream().filter(str->str.equals(packageName)).count()==1;
        }

        /**
         * 获取 class 文件数据，交由父级Classloader进行定义
         * @param classPath class文件路径
         */
        private void getClassDataToParent(String classPath){
            File classPathFile=new File(classPath);
            try(InputStream fin = new FileInputStream(classPathFile);){
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int bufferSize = 4096;
                byte[] buffer = new byte[bufferSize];
                int byteNumRead = 0;
                while ((byteNumRead = fin.read(buffer)) != -1) {
                    bos.write(buffer, 0, byteNumRead);
                }
                byte[] classBytes = bos.toByteArray();
                className = getClassName(classPathFile);
                invokeParent_defineClass(className, classBytes, 0, classBytes.length);
            } catch (IOException e) {
                throw new HotFixException(" load class File "+classPathFile.getAbsolutePath()+" fail",e);
            }
        }

        /**
         * 调用父级ClassLoader的 defineClass(String name, byte[] b, int off, int len) 方法
         */
        private void invokeParent_defineClass(String name, byte[] b, int off, int len){
            //检查是否已经加载过
            boolean loaded=false;
            try {
                parent.loadClass(name);
                loaded=true;
            } catch (ClassNotFoundException e) {
                //ignore
            }
            if(loaded) return;
            //反射调用 defineClass(String name, byte[] b, int off, int len)
            try {
                Method defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass",
                        String.class, byte[].class, int.class, int.class);
                defineClassMethod.setAccessible(true);
                defineClassMethod.invoke(parent,name,b,off,len);
            } catch (NoSuchMethodException e) {
               throw new HotFixException("no found parent ClassLoader["+ClassLoader.class.getName()+"] # " +
                       "'defineClass(String name, byte[] b, int off, int len)' method");
            } catch (IllegalAccessException e) {
               throw new HotFixException("no access permission to parent ClassLoader["+ClassLoader.class.getName()+"] # " +
                        "'defineClass(String name, byte[] b, int off, int len)' method");
            } catch (InvocationTargetException e) {
                throw new HotFixException("invoke parent ClassLoader["+ClassLoader.class.getName()+"] # " +
                        "'defineClass(String name, byte[] b, int off, int len)' method fail",e);
            }
        }

        /**
         * 获取类文件
         */
        public String getClassName(File classPathFile) {
            String dirPath = classPathFile.getParent();
            String loadPath = hotFixParams.getLoadPath();
            String packagePath=dirPath.replace(loadPath,"");
            if ("".equals(packagePath)) {
                throw new HotFixException(" please set package name path ! classPathFile = "+classPathFile.toString());
            }
            packagePath=packagePath.replace(File.separator,".");
            String className = classPathFile.getName().replace(".class", "");
            return packagePath+'.'+className;
        }

        /**
         * 加载 name 的类对象，该方法会破坏 Java 的双亲委派机制。
         * @param name 类名
         * @param resolve 如果为true，则解析该类
         */
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> cls = null;
            cls = findLoadedClass(name);
            if (cls == null) {
                try{
                    cls=getParent().loadClass(name);
                }catch (ClassNotFoundException e){
                    //ignore
                    return null;
                }
            }
            if (cls == null) {
//                throw new ClassNotFoundException(name);
                return null;
            }
            if (resolve) {
                resolveClass(cls);
            }
            return cls;
        }

        /**
         * 获取类名
         */
        public String getClassName() {
            return className;
        }


    }
}
