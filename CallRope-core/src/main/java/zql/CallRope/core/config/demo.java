package zql.CallRope.core.config;

import java.io.File;

public class demo {
    public static void main(String[] args) throws Exception {
        FileWatchService fileWatchService = new FileWatchService(new String[]{"CallRope-core/src/main/resources/rope-switch.properties"});
        fileWatchService.start();
    }
}
