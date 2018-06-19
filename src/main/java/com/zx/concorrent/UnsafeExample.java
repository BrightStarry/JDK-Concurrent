package com.zx.concorrent;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @创建者: ZhengXing
 * @创建时间： 2018/6/19
 * @描述：
 */
public class UnsafeExample {


    @SneakyThrows
    public static void main(String[] args) {
        // 反射获取Unsafe
        Unsafe unsafe = getUnsafe();


        //获取字段内存offset & 修改字段值
//        modifyField(unsafe);


        // 测试cas
        casTest(unsafe);


    }

    /**
     * 测试CAS
     * @param unsafe
     * @throws NoSuchFieldException
     */
    private static void casTest(Unsafe unsafe) throws NoSuchFieldException {
        User lockTest = new User().setSpinLock(0);
        long spinLockOff = unsafe.objectFieldOffset(User.class.getDeclaredField("spinLock"));
        boolean b = unsafe.compareAndSwapInt(lockTest, spinLockOff, 0, 1);
        System.out.println(b);
        System.out.println(lockTest);
    }

    /**
     * 获取字段内存offset & 修改字段值
     *
     * @param unsafe
     * @throws NoSuchFieldException
     */
    private static void modifyField(Unsafe unsafe) throws NoSuchFieldException {
        //定义对象
        User user = new User().setName("郑星");
        //获取对象name字段的偏移量(应该是指，相对于user对象的内存地址的偏移量)
        long nameOffset = unsafe.objectFieldOffset(User.class.getDeclaredField("name"));
        //根据内存offset,修改user的name字段的值
        unsafe.putObject(user, nameOffset, "郑牧之");
        System.out.println(user);
    }

    /**
     * 通过反射，获取Unsafe
     *
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
        //获取其单例属性
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        //将访问权限打开
        theUnsafe.setAccessible(true);
        //获取并转换
        return (Unsafe) theUnsafe.get(null);
    }

    /**
     * 测试用User类
     */
    @Accessors(chain = true)
    @Data
    public static class User {
        private String name;
        private int spinLock;
    }

}
