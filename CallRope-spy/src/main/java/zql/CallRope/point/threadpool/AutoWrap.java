package zql.CallRope.point.threadpool;

import java.util.concurrent.*;

import static zql.CallRope.point.TraceInfos.isThreadNameWithPrefix;

public class AutoWrap {
    public static Wrap autoWrap = new AutoWarpDoNothing();

    public void setAutoWrap(Wrap autoWrap) {
        this.autoWrap = autoWrap;
    }

    private static class AutoWarpDoNothing implements Wrap {
        @Override
        public Runnable doAutoWrap(Runnable runnable) {
            if(runnable == null) return null;
            Runnable ttlRunnable = TtlRunnable.get(runnable);
            return ttlRunnable;
        }

        @Override
        public <T> Callable<T> doAutoWrap(Callable<T> callable) {
            if(callable == null ) return null;
            if (!isThreadNameWithPrefix()) {
                return callable;
            }
            Callable<T> ttlCallable = TtlCallable.get(callable);
            return ttlCallable;
        }
    }

    public static Runnable doAutoWrap(Runnable runnable) {
        return autoWrap.doAutoWrap(runnable);
    }

    public static <T> Callable<T> doAutoWrap(Callable<T> callable) {
        return autoWrap.doAutoWrap(callable);
    }
}
