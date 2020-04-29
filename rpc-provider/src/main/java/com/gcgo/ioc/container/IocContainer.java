package com.gcgo.ioc.container;

import com.gcgo.ioc.annotation.MyService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class IocContainer {
    private static List<String> classNames = new ArrayList<>();//缓存类的全限定类名
    private static Map<String, Object> ioc = new HashMap<>();

    public static Map<String, Object> getIoc() {
        return ioc;
    }

    static {
        Properties properties = new Properties();
        // 使用ClassLoader加载properties配置文件生成对应的输入流
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("ioc.properties");
        try {
            // 使用properties对象加载输入流
            properties.load(in);
            //获取key对应的value值
            //1扫描类、注解
            doScan(properties.getProperty("scanPackage"));
            System.out.println("包扫描......√");
            //2初始化bean对象（实现IOC容器）
            doInstance();
            System.out.println("实例化对象......√");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void doScan(String scanPackage) {
        String scanPackagePath = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("")).getPath() +
                scanPackage.replaceAll("\\.", "/");
        File aPackage = new File(scanPackagePath);
        File[] files = aPackage.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {//如果是目录
                //递归搜索子目录
                doScan(scanPackage + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = scanPackage + "." +
                        file.getName().replaceAll(".class", "");
                classNames.add(className);
            }
        }
    }

    private static void doInstance() {
        if (classNames.size() == 0) return;
        try {
            for (String className : classNames) {
                Class<?> aClass = Class.forName(className);
                //处理service,因为service经常是实现接口的，所以为了一会方便注入，这里也缓存一下接口
                if (aClass.isAnnotationPresent(MyService.class)) {//如果添加了@MyService注解
                    MyService annotation = aClass.getAnnotation(MyService.class);
                    //获取value值
                    String beanName = annotation.value();
                    if (beanName.equals("")) {//如果没有指定id就用类名首字母小写
                        beanName = lowerFirstCharacter(aClass.getSimpleName());//类名转小写，即beanId
                    }
                    Object obj = aClass.newInstance();
                    ioc.put(beanName, obj);

                    //记录接口
                    Class<?>[] interfaces = aClass.getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        //缓存<接口名,实现类对象>
                        ioc.put(anInterface.getName(), aClass.newInstance());
                    }
                }
                //再有其他注解可以继续写else if。。。
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /*首字母转小写的方法*/
    private static String lowerFirstCharacter(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;//转小写
        return String.valueOf(chars);
    }
}
