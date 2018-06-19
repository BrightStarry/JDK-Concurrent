package com.zx.concorrent;

import lombok.SneakyThrows;

import java.lang.ref.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @创建者: ZhengXing
 * @创建时间： 2018/6/19
 * @描述： 引用相关
 */
public class ReferenExample {

    @SneakyThrows
    public static void main(String[] args) {
        /**
         * 强引用
         * java.lang.OutOfMemoryError: GC overhead limit exceeded
         * 内存溢出，因为引用之一直存在，不会被GC
         */
        //
//        ArrayList<Object> list = new ArrayList<>();
//        for (;;){
//            HashMap<Object, Object> ma = new HashMap<>();
//            ma.put("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", new Double(100000000));
//            ma.put("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", new Double(100000000));
//            ma.put("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", new Double(100000000));
//            ma.put("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", new Double(100000000));
//            list.add(ma);
//        }

        /**
         * 软引用,SoftReference
         * java.lang.NullPointerException
         * 最后抛出空指针，因为当jvm内存不足，该引用被GC,则get()方法获取到null
         */
//        SoftReference<ArrayList<Object>> s = new SoftReference<>( new ArrayList<>());
//        for (;;){
//            HashMap<Object, Object> ma = new HashMap<>();
//            ma.put("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", new Double(100000000));
//            ma.put("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", new Double(100000000));
//            ma.put("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", new Double(100000000));
//            ma.put("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", new Double(100000000));
//            s.get().add(ma);
//        }

        /**
         * 弱引用
         * java.lang.NullPointerException
         * 当map不再有对list的强引用时，list就会被gc，就会报空指针
         * 此处模拟，执行循环1000次后，将map=null，然后下次GC，list就会马上被回收
         * 注意，如果不在后面再次调用map，jvm应当会在map指定完map.put("a", new ArrayList<>());后，就可能将其GC
         */
      /*  // 构造一个list放入map -此处map维护了对list的强引用
        Map<Object, List<Object>> map = new HashMap();
        map.put("a", new ArrayList<>());
        // 将该list放入弱引用对象中
        WeakReference<List<Object>> s = new WeakReference<>(map.get("a"));
        int i = 0;
        for (; ; i++) {
            // 将map设置为null，也就是去除了所有对list的强引用，只剩下一个对list的弱引用
            if (i > 1000) {
//                map = null;
            }
            HashMap<Object, Object> ma = new HashMap<>();
            ma.put("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", new Double(100000000));
            try {
                s.get().add(ma);
            } catch (Exception e) {
                // 结束时输出此时的i
                System.out.println(i);
                // 如果不增加下面这句代码，再次使用到map。 map在执行完map.put("a", new ArrayList<>());后就会被回收
                // 所以即使没有执行map = null;，很快也会报空指针。
//                System.out.println(map.get("a"));
                System.out.println(e.getMessage());
                break;
            }
        }*/

        /**
         * 幽灵引用&引用队列
         */
        // 引用队列
        ReferenceQueue<List<Object>> q = new ReferenceQueue<>();
        new Thread(()->{
            try {
                System.out.println(q.remove());
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
        // 幽灵引用
        PhantomReference<List<Object>> p = new PhantomReference<>(new ArrayList<>(),q);
        // null
        System.out.println(p.get());
        // 主动执行gc，执行完毕后，队列将收到通知
        System.gc();
    }


}
