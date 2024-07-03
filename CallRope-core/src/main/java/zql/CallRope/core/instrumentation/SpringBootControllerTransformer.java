package zql.CallRope.core.instrumentation;

import javassist.*;
import javassist.bytecode.SignatureAttribute;
import zql.CallRope.core.util.JavassistUtils;
import zql.CallRope.spi.annotation.SPIAuto;

import java.io.IOException;

import static zql.CallRope.core.config.Configuration.getProperty;
@SPIAuto
public class SpringBootControllerTransformer implements transformer {
    public static String controller_package_prefix;

    public static final String RESTCONTROLLER_ANNOTATION = "org.springframework.web.bind.annotation.RestController";
    public static final String CONTROLLER_ANNOTATION = "org.springframework.stereotype.Controller";
    public static final String REQUESTMAPPING_ANNOTATION = "org.springframework.web.bind.annotation.RequestMapping";
    public static final String GETMAPPING_ANNOTATION = "org.springframework.web.bind.annotation.GetMapping";
    public static final String POSTTMAPPING_ANNOTATION = "org.springframework.web.bind.annotation.PostMapping";
    public static final String DELETEMAPPING_ANNOTATION = "org.springframework.web.bind.annotation.DeleteMapping";

    static {
        controller_package_prefix = getProperty("controller_package_prefix");
    }

    @Override
    public void doTransform(ClassInfo classInfo) {
        if (!classInfo.getClassName().startsWith(controller_package_prefix)) {
            return;
        }
        try {
            CtClass ctClass = classInfo.getCtClass();
            String serviceName = ctClass.getName();
            if (!ctClass.hasAnnotation(RESTCONTROLLER_ANNOTATION) && !ctClass.hasAnnotation(CONTROLLER_ANNOTATION)) {
                return;
            }
            CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
            for (CtMethod ctMethod : declaredMethods) {
                if (ctMethod.hasAnnotation(REQUESTMAPPING_ANNOTATION)
                        || ctMethod.hasAnnotation(GETMAPPING_ANNOTATION)
                        || ctMethod.hasAnnotation(POSTTMAPPING_ANNOTATION)
                        || ctMethod.hasAnnotation(DELETEMAPPING_ANNOTATION)) {
                    ClassPool classPool = ClassPool.getDefault();
                    CtClass request = classPool.get("javax.servlet.http.HttpServletRequest");
                    ctMethod.insertParameter(request);
                    String methodName = ctMethod.getName();
                    StringBuilder codeBefore = new StringBuilder();
                    codeBefore.append("\n");
                    codeBefore.append("javax.servlet.http.HttpServletRequest requestDuplicate= $1;\n");
                    codeBefore.append("String traceId = (String)requestDuplicate.getAttribute(\"CallRope-TraceId\");\n");
                    codeBefore.append("String pSpanId = (String)requestDuplicate.getAttribute(\"CallRope-pSpanId\");\n");
                    codeBefore.append("String spanId = (String)requestDuplicate.getAttribute(\"CallRope-spanId\");\n");
                    codeBefore.append(String.format("zql.CallRope.point.model.Span span = new zql.CallRope.point.model.SpanBuilder(traceId,spanId,pSpanId,\"%s\",\"%s\").build();\n", serviceName, methodName).toString());
                    codeBefore.append("zql.CallRope.point.SpyAPI.atFrameworkEnter(span, null, new String[]{\"SpringFrameworkAspectImpl\"});\n");
                    codeBefore.append("\n");
                    ctMethod.insertBefore(String.valueOf(codeBefore));

                    StringBuilder codeAfter = new StringBuilder();
                    codeAfter.append("\n");
                    codeAfter.append("zql.CallRope.point.model.Span spanDupilicate = (zql.CallRope.point.model.Span)zql.CallRope.point.TraceInfos.spanTtl.get();\n");
                    codeAfter.append("zql.CallRope.point.SpyAPI.atFrameworkExit(spanDupilicate, null, new String[]{\"SpringFrameworkAspectImpl\"});");
                    codeAfter.append("\n");
                    ctMethod.insertAfter(String.valueOf(codeAfter));
                    classInfo.flag = true;
                    classInfo.setModified();
                }
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
