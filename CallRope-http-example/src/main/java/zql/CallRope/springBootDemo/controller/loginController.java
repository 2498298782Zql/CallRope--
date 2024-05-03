package zql.CallRope.springBootDemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static zql.CallRope.springBootDemo.GlobalThreadPoolBuffer.GlabalThreadPoolUtils.threadLocal;
import static zql.CallRope.springBootDemo.GlobalThreadPoolBuffer.GlabalThreadPoolUtils.threadPoolExecutor;

@RestController
public class loginController {

    @GetMapping("/test/user/zql/114514")
    public String login(){
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                System.out.println("hello world");
                System.out.println("我是异步任务！");
            }
        });
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("exeeeeeeeee");
            }
        });
        System.out.println("睡了3秒");
        return "hello zql";
    }

    @GetMapping("/unlogin")
    public String unlogin(){
        System.out.println("退出登录");
        threadPoolExecutor.submit(() -> {
            System.out.println("hello world");
            System.out.println("退出登录");
        });
        return "success";
    }
}
