package zql.CallRope.point;

import zql.CallRope.point.model.Span;
import zql.CallRope.point.threadpool.TransmittableThreadLocal;

import java.util.Set;

import static zql.CallRope.point.config.Configuration.getPropertyAsSet;

public class TraceInfos {
    // 存取span
    public static final TransmittableThreadLocal<Span> spanTtl = new TransmittableThreadLocal<>();
    // 存取traceId
    public static final TransmittableThreadLocal<String> traceIdTtl = new TransmittableThreadLocal<>();

    // 专门存取service层数据
    public static final TransmittableThreadLocal<Span> SrvspanTtl = new TransmittableThreadLocal<>();

    public static final Set<String> threadPrefixSet;

    static {
        threadPrefixSet = getPropertyAsSet("threadpool-name-prefix");
        System.out.println(threadPrefixSet.size());
    }

    public static final boolean isThreadNameWithPrefix() {
        String currentThreadName = Thread.currentThread().getName();
        System.out.println("++++" + Thread.currentThread().getName());
        return threadPrefixSet.stream().anyMatch(currentThreadName::startsWith);
    }

    public static String getTraceId(){
        return traceIdTtl.get();
    }

    public static String getSpanId(){
        return spanTtl.get().spanId;
    }


    public static String generateChildSpanId(){
        return  getSpanId() + "." + spanTtl.get().LevelSpanId();
    }

}
