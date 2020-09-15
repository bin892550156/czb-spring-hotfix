package czb.framework.hotfix.core.helper;

import czb.framework.hotfix.core.exception.HotFixException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 引用新 Bean 对象【热修复 Bean 对象】到依赖该原 Bean 对象的 Bean 对象 的帮助类
 * @author chenzhuobin
 */
public class RefNewBeanHelper {

    /**
     * 忽略引用 热修复Bean对象 标记
     */
    public static Object IGNORE_REF_NEW_BEAN_FLAG=new Object();

    /**
     * 引用新 Bean 对象【热修复 Bean 对象】到依赖该原 Bean 对象的 Bean 对象
     * @param dependentBeanName 依赖的 Bean 名
     * @param dependentBean 依赖的 Bean 对象
     * @param toRefBeanName 要引用的 Bean 名 【热修复 Bean 的原 Bean 名】
     * @param toRefBean 要引用的 Bean 对象
     */
    public void refNewBean(String dependentBeanName, Object dependentBean, String toRefBeanName, Object toRefBean) {
        Class<?> dependentBeanClass = dependentBean.getClass();
        //找到在 dependentBean 可能引用 toRefBean 的候选 Field 对象
        Field[] declaredFields = dependentBeanClass.getDeclaredFields();
        List<Field> candidateFields=new ArrayList<>();
        for (Field field : declaredFields) {
            Class<?> declaringClass = field.getType();
            if(field.getAnnotation(Autowired.class)!=null && declaringClass.isAssignableFrom(toRefBean.getClass())){
                candidateFields.add(field);
            }
        }
        int candidateFileSize = candidateFields.size();
        if(candidateFileSize==0) return;
        //如果候选 Field 对象不只有一个，就加上 toRefBeanName 与 Field 对象名的匹配过滤
        if(candidateFields.size()!=1){
            String fieldName = getFieldName(toRefBean.getClass().getSimpleName());
            Field suitableField = null;
            for (Field field : candidateFields) {
                if(field.getName().equals(fieldName)){
                    suitableField=field;
                    break;
                }
            }
            //如果依然没有找到合适的 Field 对象，只能抛出异常。
            if(suitableField==null){
                throw new HotFixException("found more than one candidate field ,"+dependentBeanName+" dependent on "+toRefBeanName);
            }
            try {
                suitableField.setAccessible(true);
                suitableField.set(dependentBean,toRefBean);
            } catch (IllegalAccessException e) {
                throw new HotFixException("ref new bean["+toRefBeanName+"] to file["+suitableField.getName()+"] fail in dependentBeanName["+dependentBeanName
                        +"] ,cause can not access it ",e);
            }
        }else{//只有一个候选Field 的情况
            Field suitableField = candidateFields.get(0);
            try {
                suitableField.setAccessible(true);
                suitableField.set(dependentBean,toRefBean);
            } catch (IllegalAccessException e) {
                throw new HotFixException("ref new bean["+toRefBeanName+"] to file["+suitableField.getName()+"] fail in dependentBeanName["+dependentBeanName
                        +"] ,cause can not access it ",e);
            }
        }
    }

    /**
     * 找出被 {@link Autowired} 注解修饰的 Field对象的类名
     * @param hotFixObj 热修复对象
     */
    public List<String> findDependentBeanNames(Object hotFixObj){
        Field[] declaredFields = hotFixObj.getClass().getDeclaredFields();
        List<String> candidateFieldNames=new ArrayList<>();
        for (Field field : declaredFields) {
            if(field.getAnnotation(Autowired.class)!=null){
                candidateFieldNames.add(field.getType().getName());
            }
        }
        return candidateFieldNames;
    }

    /**
     * 构建 classSimpleName 最有可能的属性名
     * @param classSimpleName 简单的类名【不包含包名】
     */
    private String getFieldName(String classSimpleName){
        String uncapitalize = StringUtils.uncapitalize(classSimpleName);
        int implLastIndex = uncapitalize.lastIndexOf("Impl");
        if(implLastIndex !=-1){
            return uncapitalize.substring(0,implLastIndex);
        }
        return uncapitalize;
    }
}
