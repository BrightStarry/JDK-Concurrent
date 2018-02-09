package com.zx.concorrent;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

@SpringBootApplication
public class JDKConcurrentApplication {
	@SneakyThrows
	public static void main(String[] args) {
		/**
		 * 反射获取Unsafe
		 */
		//获取其单例属性
		Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
		//将访问权限打开
		theUnsafe.setAccessible(true);
		//获取并转换
		Unsafe unsafe = (Unsafe) theUnsafe.get(null);

		/**
		 * 获取字段内存offset & 修改字段值
		 */
		//定义对象
		User user = new User().setName("郑星");
		//获取对象name字段的偏移量
		long nameOffset = unsafe.objectFieldOffset(User.class.getDeclaredField("name"));
		//根据内存offset,修改user的name字段的值
		unsafe.putObject(user,nameOffset,"郑牧之");
		System.out.println(user);
	}

	@Accessors(chain = true)
	@Data
	public static class User{
		private String name;
	}
}
