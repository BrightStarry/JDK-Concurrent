package com.zx.concorrent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import sun.misc.Unsafe;

@SpringBootApplication
public class JDKConcurrentApplication {
	public static void main(String[] args) {
		//JDKConcurrentApplication是自定义类,在classpath中,由系统类加载器加载
		System.out.println(String.join(":","JDKConcurrentApplication的类加载器为:",JDKConcurrentApplication.class.getClassLoader().getClass().getName()));
		//这些系统提供的类,都在rt.jar中,由BootstrapClassLoader负责加载,由于该类加载器非Java编写,所以为null
		System.out.println("Object的类加载器为:" + Object.class.getClassLoader());
		//循环输出当前类加载器的父加载器,直到为null,也就是Bootstrap类加载器
		for (ClassLoader i = JDKConcurrentApplication.class.getClassLoader();
			 i != null; i = i.getParent()) {
			System.out.print(i.getClass().getName() + "->");
		}
	}
}
