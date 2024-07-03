package zql.CallRope.core.aspect.aspectImpl;

import zql.CallRope.core.aspect.FrameworkAspect;
import zql.CallRope.core.distruptor.DisruptorConfig;
import zql.CallRope.core.distruptor.DisruptorProducer;
import zql.CallRope.point.TraceInfos;
import zql.CallRope.point.model.Span;
import zql.CallRope.point.model.SpanBuilder;
import zql.CallRope.point.model.SpanEnvironment;
import zql.CallRope.spi.annotation.SPIAuto;

import java.util.Map;

@SPIAuto
public class SpringServiceAspectImpl implements FrameworkAspect {
    private DisruptorProducer<Span> producer = DisruptorConfig.createProducer(DisruptorConfig.createConsumerListener());

    @Override
    public Object entry(Span span, Map<String, Object> infos) {
        span.start = System.currentTimeMillis();
        span.setEnv(SpanEnvironment.TEST_ENVIRONMENT);
        SpanBuilder.fix(span);
        TraceInfos.SrvspanTtl.set(span);
        return span;
    }


    @Override
    public Object exit(Span span, Map<String, Object> infos) {
        span.end = System.currentTimeMillis();
        span.duration = span.end - span.start;
        producer.onData(span);
        TraceInfos.SrvspanTtl.remove();
        return span;
    }
}
