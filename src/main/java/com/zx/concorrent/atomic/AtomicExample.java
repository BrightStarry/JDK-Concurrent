package com.zx.concorrent.atomic;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * @创建者: ZhengXing
 * @创建时间： 2018/6/19
 * @描述： 原子类相关
 */
public class AtomicExample {

    public static void main(String[] args) {
        AtomicInteger atomicInteger = new AtomicInteger();
        // 获取当前值，非线程安全
        atomicInteger.get();
        // 设定为给定值
        atomicInteger.set(20);
        // 使用传入的匿名函数将 atomicInteger当前值 和 传入的x 作为参数，进行相应操作
        // 将atomicInteger更新为该操作结果，并返回该结果
        int result1 = atomicInteger.accumulateAndGet(10, (cur, x) -> {
            return cur - x;
        });
        atomicInteger.incrementAndGet();

        //
        LongAdder longAddr = new LongAdder();
    }
}
