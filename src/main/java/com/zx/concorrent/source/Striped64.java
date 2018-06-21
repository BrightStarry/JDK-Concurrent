/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.zx.concorrent.source;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;


@SuppressWarnings("serial")
abstract class Striped64 extends Number {
    /**
     * 一个简化的仅支持原始访问和CAS 的 AtomicLong
     * 使用注解避免cache伪共享
     */
    @sun.misc.Contended
    static final class Cell {
        // 实际值
        volatile long value;

        // 构造函数，传入初始实际值
        Cell(long x) {
            value = x;
        }

        // 对value属性进行cas操作
        final boolean cas(long cmp, long val) {
            return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
        }

        // Unsafe对象
        private static final sun.misc.Unsafe UNSAFE;
        // value属性的偏移量
        private static final long valueOffset;

        static {
            try {
                // 获取
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                // 获取value属性的偏移量
                Class<?> ak = Cell.class;
                valueOffset = UNSAFE.objectFieldOffset
                        (ak.getDeclaredField("value"));
            } catch (Exception e) {
                // 异常时，直接结束程序
                throw new Error(e);
            }
        }
    }

    /**
     * 获取cpu大小，用于限制 table大小
     */
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * table  非空时，大小是2的幂等
     */
    transient volatile Cell[] cells;

    /**
     * 初始值，主要在没有并发时使用，也可以用于table初始化时的回退， 通过CAS更新
     */
    transient volatile long base;

    /**
     * 一个int属性，作为自旋锁（未加锁时，值为0，加锁后值为1），通过CAS加锁
     */
    transient volatile int cellsBusy;

    /**
     * 包私有默认构造函数
     */
    Striped64() {
    }

    /**
     * 对base字段进行cas操作
     *
     * @param cmp 预期值
     * @param val 设定值
     */
    final boolean casBase(long cmp, long val) {
        // 传入该对象本身， BASE属性保存的是base属性在当前对象中的内存偏移
        return UNSAFE.compareAndSwapLong(this, BASE, cmp, val);
    }

    /**
     * 对base字段进行cas操作，从0修改为1，进行加锁操作
     */
    final boolean casCellsBusy() {
        return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    /**
     * 返回当前线程的probe值
     */
    static final int getProbe() {
        // 获取当前线程，PROBE保存了Thread中probe属性的内存偏移
        return UNSAFE.getInt(Thread.currentThread(), PROBE);
    }

    /**
     * 使用xorshift随机数算法生成新的probe，并赋给当前线程
     */
    static final int advanceProbe(int probe) {
        probe ^= probe << 13;   // xorshift 随机数算法
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        UNSAFE.putInt(Thread.currentThread(), PROBE, probe);
        return probe;
    }

    /**
     * long，累加
     * 涉及了初始化cells，扩容，操作base等
     *
     * @param x              进行更新操作的相关值(如果fn为null，则为和原值累加的值)
     * @param fn             更新函数，如果为null，则 将原值和x相加(避免了在LongAdder中需要额外的字段或函数）
     * @param wasUncontended false：在调用该方法前cas已经失败了（对于当前probe & cells。length计算出的下标的元素来说）
     */
    final void longAccumulate(long x, LongBinaryOperator fn,
                              boolean wasUncontended) {
        int h; // 当前线程的probe
        if ((h = getProbe()) == 0) { // 当线程未初始化threadLocalRandomSeed属性，则为0
            ThreadLocalRandom.current(); // 强制线程进行初始化，会生成随机的probe
            h = getProbe();//再次获取
            wasUncontended = true;//此处重新生成了随机数，假设之前对索引为0的元素进行cas失败了，那么此后会更换元素进行cas，未必失败
        }
        // 如果上一次循环 cell非空，则为true
        // 该标记唯一的作用就是，如果连续两次都更新失败了，就对cells进行扩容
        boolean collide = false;
        for (; ; ) {
            Cell[] as; // 当前的 cell数组
            Cell a; // 每个cell
            int n; // cell数组长度
            long v;// 原值
            // 如果 cells已经被初始化
            if ((as = cells) != null && (n = as.length) > 0) {
                // 随机获取一个cell，如果为空（创建新的cell）
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {       // 判断自旋锁状态，尝试增加新的cell
                        Cell r = new Cell(x);   // 乐观地创建（乐观锁）
                        if (cellsBusy == 0 && casCellsBusy()) { // 此处再获取锁
                            boolean created = false; // 是否创建成功标记
                            try {               // 双重检查模式
                                Cell[] rs;
                                int m, j;
                                // 再次判断 cells/cells.length存在，并且之前随机获取的cell仍为空
                                //（此处如果没有并发修改，则随机出的结果和外层if一样，因为h（probe）没有改变）
                                if ((rs = cells) != null &&
                                        (m = rs.length) > 0 &&
                                        rs[j = (m - 1) & h] == null) {
                                    // 赋值，并标记成功
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                // 解锁
                                cellsBusy = 0;
                            }
                            // 成功则退出循环
                            if (created)
                                break;
                            // 否则跳出该次循环
                            continue;           // 此时上个cell为非空，则collide仍旧为false
                        }
                    }
                    // 本轮循环cell为空
                    collide = false;
                } else if (!wasUncontended)       // 此标记表示，在probe未更新前，CAS已经知道会失败
                    wasUncontended = true;      // 重新hash后继续（该循环的下次操作会重新获取probe）

                    // 如果该cell不为空，并且不一定失败，则进行cas操作，预期值使用fn函数计算，或者进行相加
                else if (a.cas(v = a.value, ((fn == null) ? v + x :
                        fn.applyAsLong(v, x))))
                    // 成功则退出
                    break;
                    // 如果对cell的cas未成功，并且，cells长度超出cpu核心数了，或者cells比并发修改了，退出本次循环
                else if (n >= NCPU || cells != as)
                    collide = false;            // At max size or stale
                else if (!collide) // 如果上次循环cell非空，则将其标记为true，当下次循环再进入，并且仍旧更新失败，则会对cells扩容
                    collide = true;
                    // 尝试获取自旋锁，对cells进行扩容
                else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        // 双重检查
                        if (cells == as) {
                            // cells，长度 * 2
                            Cell[] rs = new Cell[n << 1];
                            // 重新赋值
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            cells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;
                }
                // 重新生成随机数probe
                h = advanceProbe(h);

                // 此处表示cell未被初始化
                // 如果自旋锁未被占用，并且在上个if判断到此处cells未发生变化，并且获取到了自旋锁
            } else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
                // 初始化标记，用于退出该for循环
                // 之所以有该标记，是因为下面使用了双重检查模式，即使进入了该else if代码块，初始化未必成功
                boolean init = false;
                try {
                    // 再次判断（因为在else if (cellsBusy == 0 && cells == as && casCellsBusy()) 处， 没有加锁），
                    // 在判断完cells == as 后才去获取锁，如果那时cells被其他线程修改了，还是可能会失败.
                    // 双重检查模式
                    if (cells == as) {
                        // 初始大小为2，2^1
                        Cell[] rs = new Cell[2];
                        // 随机生成第0个或第一个元素
                        // 与操作，当随机数probe的第一位为1，则为1，否则为0
                        // 创建一个cell，传入要累加的值
                        rs[h & 1] = new Cell(x);
                        // 赋值
                        cells = rs;
                        // 初始化标记
                        init = true;
                    }
                } finally {
                    // 解锁
                    cellsBusy = 0;
                }
                // 初始化成功后，退出
                if (init)
                    break;

                // 对base进行cas操作，预期值使用fn函数计算，或者进行相加
            } else if (casBase(v = base, ((fn == null) ? v + x :
                    fn.applyAsLong(v, x))))
                // 如果成功，退出
                break;                          // 重新使用base
        }
    }

    /**
     * Same as longAccumulate, but injecting long/double conversions
     * in too many places to sensibly merge with long version, given
     * the low-overhead requirements of this class. So must instead be
     * maintained by copy/paste/adapt.
     */
    final void doubleAccumulate(double x, DoubleBinaryOperator fn,
                                boolean wasUncontended) {
        int h;
        if ((h = getProbe()) == 0) {
            ThreadLocalRandom.current(); // force initialization
            h = getProbe();
            wasUncontended = true;
        }
        boolean collide = false;                // True if last slot nonempty
        for (; ; ) {
            Cell[] as;
            Cell a;
            int n;
            long v;
            if ((as = cells) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {       // Try to attach new Cell
                        Cell r = new Cell(Double.doubleToRawLongBits(x));
                        if (cellsBusy == 0 && casCellsBusy()) {
                            boolean created = false;
                            try {               // Recheck under lock
                                Cell[] rs;
                                int m, j;
                                if ((rs = cells) != null &&
                                        (m = rs.length) > 0 &&
                                        rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;           // Slot is now non-empty
                        }
                    }
                    collide = false;
                } else if (!wasUncontended)       // CAS already known to fail
                    wasUncontended = true;      // Continue after rehash
                else if (a.cas(v = a.value,
                        ((fn == null) ?
                                Double.doubleToRawLongBits
                                        (Double.longBitsToDouble(v) + x) :
                                Double.doubleToRawLongBits
                                        (fn.applyAsDouble
                                                (Double.longBitsToDouble(v), x)))))
                    break;
                else if (n >= NCPU || cells != as)
                    collide = false;            // At max size or stale
                else if (!collide)
                    collide = true;
                else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        if (cells == as) {      // Expand table unless stale
                            Cell[] rs = new Cell[n << 1];
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            cells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;                   // Retry with expanded table
                }
                h = advanceProbe(h);
            } else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
                boolean init = false;
                try {                           // Initialize table
                    if (cells == as) {
                        Cell[] rs = new Cell[2];
                        rs[h & 1] = new Cell(Double.doubleToRawLongBits(x));
                        cells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            } else if (casBase(v = base,
                    ((fn == null) ?
                            Double.doubleToRawLongBits
                                    (Double.longBitsToDouble(v) + x) :
                            Double.doubleToRawLongBits
                                    (fn.applyAsDouble
                                            (Double.longBitsToDouble(v), x)))))
                break;                          // Fall back on using base
        }
    }

    // Unsafe
    private static final sun.misc.Unsafe UNSAFE;
    // base的内存偏移
    private static final long BASE;
    // cellsBusy的内存偏移
    private static final long CELLSBUSY;
    // Thread的probe的内存偏移
    private static final long PROBE;

    static {
        try {
            // 获取 Unsafe和各个内存偏移
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> sk = Striped64.class;
            BASE = UNSAFE.objectFieldOffset
                    (sk.getDeclaredField("base"));
            CELLSBUSY = UNSAFE.objectFieldOffset
                    (sk.getDeclaredField("cellsBusy"));
            Class<?> tk = Thread.class;
            PROBE = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomProbe"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
