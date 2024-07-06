package zql.CallRope.core.config;

import zql.CallRope.core.aspect.SpyImpl;
import zql.CallRope.point.DoNothingSpy;
import zql.CallRope.point.IDutils.ShardedTokenBucket;
import zql.CallRope.point.IDutils.TraceIdGenerator;
import zql.CallRope.point.SpyAPI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.FileInputStream;
import java.util.Properties;

// 观察者模式
public class FileWatchService implements Runnable {
    private final List<String> watchFiles;
    private final List<String> filesCurrentHash;
    private final Listener listener;
    private static final int WATCH_INTEVAL = 500;
    private MessageDigest md = MessageDigest.getInstance("MD5");
    private volatile boolean stopped = false;
    private final ScheduledExecutorService executorService;

    public FileWatchService(String[] watchFiles, Listener listener) throws Exception {
        this.listener = listener;
        this.watchFiles = new ArrayList<>();
        this.filesCurrentHash = new ArrayList<>();
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        for (int i = 0; i < watchFiles.length; i++) {
            // 判断文件路径是否为空且文件是否存在
            if (null != watchFiles[i] && watchFiles[i].trim().length() > 0 && new File(watchFiles[i]).exists()) {
                this.watchFiles.add(watchFiles[i]);
                this.filesCurrentHash.add(hash(watchFiles[i]));
            }
        }
    }

    public FileWatchService(String[] watchFiles) throws Exception {
        this(watchFiles, path -> {
            try (FileInputStream fis = new FileInputStream(path)) {
                Properties properties = new Properties();
                properties.load(fis);
                String traceSwitch = properties.getProperty("TraceSwitch");
                if ("true".equalsIgnoreCase(traceSwitch)) {
                    System.out.println("Trace is true");
                    // TODO add to log
                    SpyAPI.setSpy(new SpyImpl());
                } else if ("false".equalsIgnoreCase(traceSwitch)) {
                    System.out.println("Trace is false");
                    // TODO add to log
                    SpyAPI.setSpy(new DoNothingSpy());
                } else {
                    // TODO change to log
                    System.out.println("TraceSwitch is not set or has an invalid value");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String hash(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        md.update(Files.readAllBytes(path));
        // 计算hash值
        byte[] hash = md.digest();
        // 讲hash数组转化为16进制的字符串
        return UtilAll.bytes2string(hash);
    }

    @Override
    public void run() {
        if (this.isStopped()) {
            return;
        }
        try {
            for (int i = 0; i < watchFiles.size(); i++) {
                String newHash;
                try {
                    newHash = hash(watchFiles.get(i));
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                    continue;
                }
                if (!newHash.equals(filesCurrentHash.get(i))) {
                    filesCurrentHash.set(i, newHash);
                    listener.onChanged(watchFiles.get(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void start() {
        // TODO add to log
        executorService.scheduleAtFixedRate(this, 0, WATCH_INTEVAL, TimeUnit.MILLISECONDS);
    }

    public boolean isStopped() {
        return stopped;
    }

    public void stop() {
        this.stopped = false;
    }

    public interface Listener {
        void onChanged(String path);
    }
}
