package zql.CallRope.point.threadpool;

import zql.CallRope.point.SpyAPI;
import zql.CallRope.point.TraceInfos;
import zql.CallRope.point.model.Span;
import zql.CallRope.point.model.SpanBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static zql.CallRope.point.TraceInfos.isThreadNameWithPrefix;
import static zql.CallRope.point.threadpool.TransmittableThreadLocal.Transmitter.*;

public class TtlRunnable implements Runnable, TtlEnhanced {
    private final AtomicReference<Object> capturedRef;
    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    private Runnable runnable;
    private final boolean releaseTtlValueReferenceAfterRun;

    private TtlRunnable(Runnable runnable, boolean releaseTtlValueReferenceAfterRun) {
        this.capturedRef = new AtomicReference<>(capture());
        this.runnable = runnable;
        this.releaseTtlValueReferenceAfterRun = releaseTtlValueReferenceAfterRun;
    }

    @Override
    public void run() {
        if (!isThreadNameWithPrefix()) {
            runnable.run();
            return;
        }
        System.out.println("$$$$$$$$$$$$$$$$$$$$$" + Thread.currentThread().getName());
        Object captured = capturedRef.get();
        if (captured == null && releaseTtlValueReferenceAfterRun && !capturedRef.compareAndSet(captured, null)) {
            throw new IllegalStateException("TTL value reference is released after run!");
        }
        Object backup = replay(captured);
        try {
            Span oldSpan = (Span) TraceInfos.spanTtl.get();
            // 真正的Runnable调用
            if (oldSpan != null) {
                Span span = new SpanBuilder(oldSpan.traceId, oldSpan.spanId + "." + oldSpan.LevelSpanId(), oldSpan.spanId, oldSpan.serviceName, oldSpan.methodName).withIsAsyncThread(true).build();
                TraceInfos.spanTtl.set(span);
                SpyAPI.atFrameworkEnter(span, null, new String[]{"SpringFrameworkAspectImpl"});
            }
            runnable.run();
            oldSpan = TraceInfos.spanTtl.get();
            if ( oldSpan != null) {
                SpyAPI.atFrameworkExit(oldSpan, null, new String[]{"SpringFrameworkAspectImpl"});
            }
        } finally {
            restore(backup);
        }
    }

    public static Runnable get(Runnable runnable) {
        return create(runnable, false);
    }

    public static TtlRunnable create(Runnable runnable, boolean releaseTtlValueReferenceAfterRun) {
        if (null == runnable) return null;
        if (runnable instanceof TtlEnhanced) {
            throw new IllegalStateException("Already TtlRunnable!");
        }
        return new TtlRunnable(runnable, releaseTtlValueReferenceAfterRun);
    }
}