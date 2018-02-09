package com.zx.concorrent.loader;

/**
 * author:ZhengXing
 * datetime:2018/2/9 0009 14:05
 * 自定义类加载器
 */
public class CustomClassLoader extends ClassLoader{

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return super.loadClass(name, resolve);
	}
}
