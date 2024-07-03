package zql.CallRope.core.instrumentation;


import javassist.*;
import zql.CallRope.spi.annotation.SPIAuto;

import java.io.IOException;

import static zql.CallRope.core.config.Configuration.getProperty;

@SPIAuto
public class SpringServiceTransformer implements transformer{

    public static final String SERVICE_ANNOTATION = "org.springframework.stereotype.Service";
    public static String service_package_prefix;

    static{
        service_package_prefix = getProperty("service_package_prefix");
    }

    @Override
    public void doTransform(ClassInfo classInfo) throws IOException, NotFoundException, ClassNotFoundException, CannotCompileException {

        if (!classInfo.getClassName().startsWith(service_package_prefix)) {
            return;
        }
        try {
            CtClass ctClass = classInfo.getCtClass();
            String serviceName = ctClass.getName();
            if (!ctClass.hasAnnotation(SERVICE_ANNOTATION)) {
                return;
            }
            CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
            for (CtMethod ctMethod : declaredMethods) {
                String methodName = ctMethod.getName();
                StringBuilder codeBefore = new StringBuilder();
                codeBefore.append("{\n");
                codeBefore.append("String traceId = zql.CallRope.point.TraceInfos.getTraceId();\n");
                codeBefore.append("String pSpanId = zql.CallRope.point.TraceInfos.getSpanId();\n");
                codeBefore.append("String spanId = zql.CallRope.point.TraceInfos.generateChildSpanId();");
                codeBefore.append(String.format("zql.CallRope.point.model.Span span = new zql.CallRope.point.model.SpanBuilder(traceId, spanId, pSpanId, " +
                        "\"%s\", \"%s\").build();\n", serviceName, methodName));
                codeBefore.append("zql.CallRope.point.SpyAPI.atFrameworkEnter(span, null, new String[]{\"SpringServiceAspectImpl\"});\n");
                codeBefore.append("zql.CallRope.point.TraceInfos.SrvspanTtl.set(span);\n");
                codeBefore.append("}\n");
                ctMethod.insertBefore(codeBefore.toString());
                StringBuilder codeAfter = new StringBuilder();
                codeAfter.append("\n");
                codeAfter.append("zql.CallRope.point.model.Span spanDuplicate = (zql.CallRope.point.model.Span)zql.CallRope.point.TraceInfos.SrvspanTtl.get();\n");
                codeAfter.append("zql.CallRope.point.SpyAPI.atFrameworkExit(spanDuplicate, null, new String[]{\"SpringServiceAspectImpl\"});");
                codeAfter.append("\n");
                ctMethod.insertAfter(codeAfter.toString());
                classInfo.flag = true;
                classInfo.setModified();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }

    }
}
