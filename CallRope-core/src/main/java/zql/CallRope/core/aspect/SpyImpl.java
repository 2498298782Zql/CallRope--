package zql.CallRope.core.aspect;
import zql.CallRope.core.aspect.aspectImpl.SpringFrameworkAspectImpl;
import zql.CallRope.core.aspect.aspectSPI.aspectManager;
import zql.CallRope.core.config.FileWatchService;
import zql.CallRope.point.SpyAPI;
import zql.CallRope.point.SpySPI;
import zql.CallRope.point.model.Span;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static zql.CallRope.core.config.Configuration.getProperty;

public class SpyImpl implements SpySPI {
    private volatile static Map<String, MethodAspect> methodAspects;

    private volatile static Map<String, FrameworkAspect> frameworkAspects;

    private volatile static Map<String, AsyncThreadAspect> asyncThreadAspects;


    public static void init() {
        methodAspects = createMethodAspectsMap(methodAspects);
        frameworkAspects = createFrameworkAspectsMap(frameworkAspects);
        asyncThreadAspects = createAsyncThreadAspectsMap(asyncThreadAspects);
        aspectManager.init(methodAspects,frameworkAspects, asyncThreadAspects);
        SpyAPI.setSpy(new SpyImpl());
        try {
            // 开启调用链开关监视
            FileWatchService fileWatchService = new FileWatchService(new String[]{getProperty("trace.switch")});
            fileWatchService.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void atEnter(Class<?> clazz, String methodInfo, Object target, Map<String, Object> infos) {
        try {
            String[] methodInfos = splitMethodInfo(methodInfo);
            String methodName = methodInfos[0];
            String methodDesc = methodInfos[1];
            for (Map.Entry<String, MethodAspect> methodAspectEntry: methodAspects.entrySet()) {
                methodAspectEntry.getValue().before(clazz, methodName, methodDesc, target, infos);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void atExit(Class<?> clazz, String methodInfo, Object target, Object returnObject, Map<String, Object> infos) {
        try {
            String[] methodInfos = splitMethodInfo(methodInfo);
            String methodName = methodInfos[0];
            String methodDesc = methodInfos[1];
            for (Map.Entry<String, MethodAspect> methodAspectEntry: methodAspects.entrySet()) {
                methodAspectEntry.getValue().after(clazz, methodName, methodDesc, target, returnObject, infos);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void atExceptionExit(Class<?> clazz, String methodInfo, Object target, Map<String, Object> infos, Throwable throwable) {
        try {
            String[] methodInfos = splitMethodInfo(methodInfo);
            String methodName = methodInfos[0];
            String methodDesc = methodInfos[1];
            for (Map.Entry<String, MethodAspect> methodAspectEntry: methodAspects.entrySet()) {
                methodAspectEntry.getValue().error(clazz, methodName, methodDesc, target, infos, throwable);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void atFrameworkEnter(Span span, Map<String, Object> infos, String[] enhanceCLassnames) {
        for(String enhance : enhanceCLassnames){
            if(frameworkAspects.containsKey(enhance)){
                frameworkAspects.get(enhance).entry(span, infos);
            }
        }
    }

    @Override
    public void atFrameworkExit(Span span, Map<String, Object> infos, String[] enhanceCLassnames) {
        for(String enhance: enhanceCLassnames){

            if(frameworkAspects.containsKey(enhance)){
                frameworkAspects.get(enhance).exit(span, infos);
            }
        }
    }

    @Override
    public void atFrameThreadPoolEnter(Span span, String[] enhanceCLassnames) {
        for(String enhance: enhanceCLassnames){
            if(asyncThreadAspects.containsKey(enhance)){
                asyncThreadAspects.get(enhance).enter(span, true, null);
            }
        }
    }

    @Override
    public void atFrameThreadPoolExit(Span span, String[] enhanceCLassnames) {
        for(String enhance: enhanceCLassnames){
            if(asyncThreadAspects.containsKey(enhance)){
                asyncThreadAspects.get(enhance).exit(span, true, null);
            }
        }
    }

    private String[] splitMethodInfo(String methodInfo) {
        return methodInfo.split(Pattern.quote("|"));
    }


    private static Map<String, MethodAspect> createMethodAspectsMap(Map<String, MethodAspect> map) {
        if (map == null) {
            synchronized (SpyImpl.class) {
                if (map == null) {
                    return new HashMap<String, MethodAspect>();
                }
            }
        }
        return map;
    }

    private static Map<String, FrameworkAspect> createFrameworkAspectsMap(Map<String, FrameworkAspect> map) {
        if (map == null) {
            synchronized (SpyImpl.class) {
                if (map == null) {
                    return new HashMap<String, FrameworkAspect>();
                }
            }
        }
        return map;
    }

    private static Map<String, AsyncThreadAspect> createAsyncThreadAspectsMap(Map<String, AsyncThreadAspect> map) {
        if (map == null) {
            synchronized (SpyImpl.class) {
                if (map == null) {
                    return new HashMap<String, AsyncThreadAspect>();
                }
            }
        }
        return map;
    }

/*
    // 技术验证
    public static void main(String[] args) {
        init();
        System.out.println("初始化完成");
        Collection<FrameworkAspect> values = frameworkAspects.values();
        for(FrameworkAspect frameworkAspect : values){
            frameworkAspect.say();
        }
        System.out.println("输出结束");
    }
*/

}
