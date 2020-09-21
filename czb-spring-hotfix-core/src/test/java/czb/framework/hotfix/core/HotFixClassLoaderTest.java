package czb.framework.hotfix.core;


import czb.framework.hotfix.core.classloader.HotFixClassLoader;
import czb.framework.hotfix.core.config.HotFixProperties;
import czb.framework.hotfix.core.exception.HotFixException;
import czb.framework.hotfix.core.service.BinService;
import czb.framework.hotfix.core.service.CzbService;
import czb.framework.hotfix.core.service.impl.BinServiceImpl;
import czb.framework.hotfix.core.service.impl.CzbServiceImpl;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class HotFixClassLoaderTest {

    private HotFixProperties hotFixParams;

    public HotFixClassLoaderTest(){
        hotFixParams =new HotFixProperties();
        hotFixParams.setBasePackage("czb.framework.hotfix.core");
        hotFixParams.setLoadPath("E:\\Project\\Java\\OpenSource\\czb-spring-hotfix\\czb-spring-hotfix-core\\hotfix\\");
    }


    @Test
    public void test_HotFixClassLoader(){
        System.out.println("Before hotfix...");
        CzbService czbService=new CzbServiceImpl();
        czbService.setBinService(new BinServiceImpl());
        List<String> names = czbService.listName();
        System.out.println(names);

        System.out.println();

        System.out.println("After hofix...");
        BinService binService=czbService.getBinService();
        ClassLoader classLoader= new HotFixClassLoader(CzbService.class.getClassLoader(), hotFixParams);
        try {
            Class<?> newCzbServiceImplClass = classLoader.loadClass(CzbServiceImpl.class.getName());
            czbService = (CzbService) newCzbServiceImplClass.newInstance();
            czbService.setBinService(binService);
            System.out.println(czbService.listName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_getClassName(){
        File classPathFile=new File("E:\\Project\\Java\\OpenSource\\czb-spring-hotfix\\czb-spring-hotfix-core\\hotfix\\czb\\framework\\hotfix\\core\\service\\impl\\CzbServiceImpl.class");
        String dirPath = classPathFile.getParent();
        String loadPath = hotFixParams.getLoadPath();
        String packagePath=dirPath.replace(loadPath,"");
        if ("".equals(packagePath)) {
            throw new HotFixException(" please set package name path ! classPathFile = "+classPathFile.toString());
        }
        packagePath=packagePath.replace(File.separator,".");
        String className = classPathFile.getName().replace(".class", "");
        System.out.println(packagePath+'.'+className);
    }

    private String loadPath;
    private String getLoadPath(){
        if(loadPath!=null) return loadPath;
        loadPath = hotFixParams.getLoadPath();
        if(loadPath.endsWith(File.separator)){
            return loadPath;
        }else{
            loadPath= loadPath+File.separator;
            return loadPath;
        }
    }
}
