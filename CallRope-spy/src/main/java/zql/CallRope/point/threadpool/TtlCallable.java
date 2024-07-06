package zql.CallRope.point.threadpool;

import zql.CallRope.point.SpyAPI;
import zql.CallRope.point.TraceInfos;
import zql.CallRope.point.model.Span;
import zql.CallRope.point.model.SpanBuilder;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static zql.CallRope.point.TraceInfos.isThreadNameWithPrefix;
import static zql.CallRope.point.threadpool.TransmittableThreadLocal.Transmitter.*;

public class TtlCallable<V> implements TtlEnhanced, Callable<V> {
    private final AtomicReference<Object> capturedRef;
    private final Callable<V> callable;
    private final boolean releaseTtlValueReferenceAfterCall;

    private TtlCallable(Callable<V> callable, boolean releaseTtlValueReferenceAfterCall) {
        this.capturedRef = new AtomicReference<>(capture());
        this.callable = callable;
        this.releaseTtlValueReferenceAfterCall = releaseTtlValueReferenceAfterCall;
    }

    @Override
    public V call() throws Exception {
        final Object captured = capturedRef.get();
        if (captured == null || releaseTtlValueReferenceAfterCall && !capturedRef.compareAndSet(captured, null)) {
            throw new IllegalStateException("TTL value reference is released after call!");
        }
        final Object backup = replay(captured);
        try {
            Span oldSpan = (Span) TraceInfos.spanTtl.get();
            if (oldSpan != null) {
                Span span = new SpanBuilder(oldSpan.traceId, oldSpan.spanId + "." + oldSpan.LevelSpanId(), oldSpan.spanId, oldSpan.serviceName, oldSpan.methodName).withIsAsyncThread(true).build();
                TraceInfos.spanTtl.set(span);
                SpyAPI.atFrameworkEnter(span, null, new String[]{"SpringFrameworkAspectImpl"});
            }
            V result = callable.call();
            oldSpan = TraceInfos.spanTtl.get();
            if (oldSpan != null) {
                SpyAPI.atFrameworkExit(oldSpan, null, new String[]{"SpringFrameworkAspectImpl"});
            }
            return result;
        } finally {
            restore(backup);
        }
    }

    public static <V> Callable<V> get(Callable<V> callable) {
        if (!isThreadNameWithPrefix()) {
            return callable;
        }
        return create(callable, false);
    }

    public static <V> TtlCallable create(Callable<V> callable, boolean releaseTtlValueReferenceAfterRun) {
        if (null == callable) return null;
        if (callable instanceof TtlEnhanced) {
            throw new IllegalStateException("Already TtlCallable!");
        }
        return new TtlCallable(callable, releaseTtlValueReferenceAfterRun);
    }


}
