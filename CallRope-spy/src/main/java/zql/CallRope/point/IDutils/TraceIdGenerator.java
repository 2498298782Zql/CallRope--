package zql.CallRope.point.IDutils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

public class TraceIdGenerator {
    private static final AtomicInteger atomicInteger = new AtomicInteger(1);

    public static final ShardedTokenBucket tokenBucket;

    static {
        tokenBucket = new ShardedTokenBucket(5, 100,5);
    }
    public static String generateTraceId() {
        // 获取当前时间的毫秒数
        long timestamp = System.currentTimeMillis();

        // 获取当前机器的 IP 地址，并转换为十六进制表示
        String ipAddress = getIpAddressHex();

        // 获取当前进程的 PID，并转换为十六进制表示
        String processId = getProcessIdHex();

        // 获取业务运行环境标识
        String environment = "d"; // 默认为线上环境

        // 获取自增 ID，并转换为十六进制表示
        String atomicId = String.format("%04x", atomicInteger.getAndIncrement() % 10000);

        // 拼接各个部分得到 traceId
        String traceId = processId + ipAddress + environment + timestamp + atomicId;

        return traceId;
    }

    private static String getIpAddressHex() {
        try {
            // 获取本机 IP 地址
            InetAddress inetAddress = InetAddress.getLocalHost();
            byte[] addressBytes = inetAddress.getAddress();

            // 将 IP 地址转换为十六进制表示
            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : addressBytes) {
                hexBuilder.append(String.format("%02x", b));
            }

            return hexBuilder.toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "00000000"; // 无法获取 IP 地址时返回默认值
        }
    }

    private static String getProcessIdHex() {
        // 获取当前进程的 PID
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        // 获取进程 ID
        String processName = runtimeMXBean.getName();
        long pid = Long.parseLong(processName.split("@")[0]);

        // 将 PID 转换为十六进制表示
        return String.format("%04x", pid);
    }


    public static String tryGenerateTraceId(){
        if(tokenBucket.tryConsume(1)) {
            return generateTraceId();
        }
        return "-1";
    }

}

