package zql.CallRope.point;

import zql.CallRope.point.model.Span;

import java.util.Map;

public final class DoNothingSpy implements SpySPI {

    public DoNothingSpy() {
    }

    @Override
    public void atEnter(Class<?> clazz, String methodInfo, Object target, Map<String, Object> infos) {
    }

    @Override
    public void atExit(Class<?> clazz, String methodInfo, Object target, Object returnObject, Map<String, Object> infos) {
    }

    @Override
    public void atExceptionExit(Class<?> clazz, String methodInfo, Object target, Map<String, Object> infos, Throwable throwable) {
    }

    @Override
    public void atFrameworkEnter(Span span, Map<String, Object> infos, String[] enhanceCLassnames) {

    }

    @Override
    public void atFrameworkExit(Span span, Map<String, Object> infos, String[] enhanceCLassnames) {

    }

    @Override
    public void atFrameThreadPoolEnter(Span span, String[] enhanceCLassnames) {

    }

    @Override
    public void atFrameThreadPoolExit(Span span, String[] enhanceCLassnames) {

    }
}
