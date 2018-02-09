### JDK-Concurrent 深入学习
开始从源码层面研究Concurrent包,以及其他并发相关的包,对于途中遇到的不明白的东西,也会作相应了解.

#### 强引用、软引用、弱引用、幽灵引用、引用队列
- 强引用:StrongReference
    - 例如Date date = new Date()；对象可以在程序中到处传递；
    - 强引用限制了对象在内存中的存活时间；例如A对象中保存了B对象的强引用，那么如果
        A对象没有把B对象设为null的话，只有当A对象被回收后，B对象不再指向它了，才可能被回收.
- 软引用:SoftReference
    - 当JVM内存不足时，可以回收软引用对象，如果还不足才抛出OOM(OutOfMemory)；
    - 该引用非常适合创建缓存；
    - 注意，因为该对象可能被回收，所以每次get时，需要判断是否存在
    - 在JDK 1.2之后，提供了SoftReference类来实现软引用。
- 弱引用:WeakReference
    - 引用一个对象，但是并不阻止该对象被回收
    - 在垃圾回收器运行的时候，如果一个对象的所有引用都是弱引用的话，该对象会被回收
    - 弱引用的作用在于解决强引用所带来的对象之间在存活时间上的耦合关系
    - 常用于集合类，例如HashMap中(有WeakHashMap类)；
    - 在JDK 1.2之后，提供了WeakReference类来实现弱引用。
    - (如下类加载器中的并发类加载器注册逻辑就使用了该引用,以在类加载器GC后,自动GC该类加载器的是否并发状态)  
- 幽灵引用:PhantomReference
    - 任何时候调用get，返回的都是null，需要搭配引用队列使用
    - PhantomReference ref = new PhantomReference(new A(), queue); 这么写可以确保A对象完全被回收后才进入引用队列
    - 在创建幽灵引用PhantomReference的时候必须要指定一个引用队列。  
        当一个对象的finalize方法已经被调用了之后，这个对象的幽灵引用会被加入到队列中。  
        通过检查该队列里面的内容就知道一个对象是不是已经准备要被回收了。  
    - 幽灵引用及其队列的使用情况并不多见，主要用来实现比较精细的内存使用控制，这对于移动设备来说是很有意义的。
        程序可以在确定一个对象要被回收之后，再申请内存创建新的对象。通过这种方式可以使得程序所消耗的内存维持在一个相对较低的数量
    - 在JDK 1.2之后，提供了PhantomReference类来实现幽灵引用。
- 引用队列:ReferenceQueue
    - 在有些情况下，程序会需要在一个对象的可达到性发生变化的时候得到通知。  
    比如某个对象的强引用都已经不存在了，只剩下软引用或是弱引用。但是还需要对引用本身做一些的处理。  
    典型的情景是在哈希表中。引用对象是作为WeakHashMap中的键对象的，当其引用的实际对象被垃圾回收之后，  
    就需要把该键值对从哈希表中删除。有了引用队列（ReferenceQueue），就可以方便的获取到这些弱引用对象，  
    将它们从表中删除。在软引用和弱引用对象被添加到队列之前，其对实际对象的引用会被自动清空。  
    通过引用队列的poll/remove方法就可以分别以非阻塞和阻塞的方式获取队列中的引用对象。

#### 双亲委派模型
- 对于JVM来说,只有两种类加载器
    - 启动类加载器(Bootstrap ClassLoader): 由C++语言实现（针对HotSpot）,负责将存放在<JAVA_HOME>\lib目录或-Xbootclasspath参数指定的路径中的类库加载到内存中（比如rt.jar）
    - 其他类加载器: 由Java实现,并继承java.lang.ClassLoader.
- 或者更细分为
    - 启动类加载器(Bootstrap ClassLoader)
    - 扩展类加载器(Extension ClassLoader): 负责加载%JAVA_HOME%/lib/ext中的所有类库,由 Sun 的 ExtClassLoader（sun.misc.Launcher$ExtClassLoader）实现
    - 应用程序加载器(Application ClassLoader): 负责加载classpath中的所有类库.由 Sun 的 AppClassLoader（sun.misc.Launcher$AppClassLoader）实现
        由于该类加载器是ClassLoader中的getSystemClassLoader()方法的返回值，因此被称为系统（System）加载器。
- 双亲委派模型:
    - 该模型要求除了BootstrapClassLoader外,其余的类加载器都需要有自己的父类加载器.
    - 子类加载器通过组合(<Java编程思想>中提及过,也就是子类加载器将父类加载器作为自己的成员变量)而非继承复用父类加载器的代码.
    - 在某个类加载器加载class文件时,它首先通过父类加载器去加载(这样依次传递到顶层类加载器(BootstrapClassLoader)),如果父类加载器加载不了(它的搜索范围中找不到该类),子类加载器才会去尝试加载该类.
- 作用
    - 防止重复加载同个类.
    - 防止恶意加载同名类.(例如 自定义java.lang.String类,该类永远无法被加载,因为最顶层的类加载器首先加载了系统的String类
        (应该是指,虽然顶层加载器因已经加载而无法加载自定义String类,则子加载器无法重复加载).即使在自定义的加载器中强制加载(不调用父加载器去尝试,也就是违反双亲委派原则),
        由于两个String类的类加载器不同,这两个类也不会等同(判断一个对象的类型时,还会比较该类型(String类型)的类加载器)).
- 如下代码
    ```java
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
    ```
    将输出
    ```
    JDKConcurrentApplication的类加载器为::sun.misc.Launcher$AppClassLoader
    Object的类加载器为:null
    sun.misc.Launcher$AppClassLoader->sun.misc.Launcher$ExtClassLoader->
    ```

- java.lang.ClassLoader类: 类加载器的加载流程通常为 loadClass -> findClass -> defineClass
    - loadClass()方法
        ```
              protected Class<?> loadClass(String name, boolean resolve)
                      throws ClassNotFoundException
                  {
                      //getClassLoadingLock(name)方法会返回两类Object作为锁.
                      //当parallelLockMap参数不为空的时候,会针对每个className创建单独的Object作为锁,以达到并发加载的效果
                      //当parallelLockMap为空时,直接使用this,也就是这个类加载器作为锁,只能依次加载class
                      //而parelleLockMap是否会被赋值,则通过判断ClassLoader.ParallelLoaders的loaderTypes中以类加载器类型为key的boolean类型value来判断
                      //loaderTypes中包含了一个封装了WeakHashMap<Class<? extends ClassLoader>, Boolean>())的SetFromMap(该类在Collections中),
                      //它的add()方法默认通过被组合在其中WeakHashMap的put方法,key为add传入的参数,value为true.(所以,类加载器只要调用其注册方法,就被加入该集合,并认定为并发加载器)
                      //所以,其真实判断方法是,只要loaderTypes中包含了当前类加载器的key,就作为并发类加载器.
                      //此外,loaderTypes组合的是WeakHashMap,根据我的理解,这很好的避免了其他类加载器需要将自己注册为并发记载器,而忘记了取消注册的情况.
                      //因为当一个A类加载器将自己注册为并发加载器,开始加载class时,此时A类加载在WeakHashMap外还包含一个强引用,所以并不会被GC,
                      //而当A类加载器完成加载被GC后,该WeakHashMap的对应元素也会被自动GC.
                      //系统类加载器(AppClassLoader)的父类(URLClassLoader)就调用过注册方法ClassLoader.registerAsParallelCapable(),
                      //那么,系统类加载器应该不是并发加载器.因为它通过this.getClass()获取类加载器的Class后,使用集合的contains()方法equals比较,而非使用isAssignableFrom()
                      synchronized (getClassLoadingLock(name)) {
                          // 首先,检查类是否已经被加载
                          //该方法首先判断name是否符合规范,然后调用JNI方法尝试获取已经被加载的类
                          Class<?> c = findLoadedClass(name);
                          //如果未加载
                          if (c == null) {
                              //获取当前纳秒数
                              long t0 = System.nanoTime();
                              try {
                                  //调用父类加载器
                                  if (parent != null) {
                                      c = parent.loadClass(name, false);
                                  } else {
                                      //或从Bootstrrap 类加载器中查找该类(JNI方法)
                                      c = findBootstrapClassOrNull(name);
                                  }
                              } catch (ClassNotFoundException e) {
                                  //忽略此处可能抛出的ClassNotFoundException
                              }
                              
                              //
                              if (c == null) {
                                  //如果此时该类还未找到,
                                  // If still not found, then invoke findClass in order
                                  // to find the class.
                                  long t1 = System.nanoTime();
                                  //调用该类加载自己的加载方法
                                  c = findClass(name);
              
                                  // this is the defining class loader; record the stats 记录加载数据
                                  sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                                  sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                                  sun.misc.PerfCounter.getFindClasses().increment();
                              }
                          }
                          //加载后是否解析
                          if (resolve) {
                              //解析该加载的类
                              resolveClass(c);
                          }
                          return c;
                      }
                  }  
        ```
    
    - findClass()方法
        - 该类默认方法抛出异常,需要自行实现逻辑
            ```
                  protected Class<?> findClass(String name) throws ClassNotFoundException {
                      throw new ClassNotFoundException(name);
                  }
            ```
        - 

#### Unsafe : 通过JNI提供硬件级别的原子操作
- 在看CopyOnWriteArrayList/AtomicInteger等类源码时,都出现了该对象,都是JNI方法,不解其意,特地学习下.
- 该类在sun.misc包下,不属于java标准,但在JDK/Netty/Hadoop/Kafka中都有对其的使用.
- Unsafe类使Java拥有了像C语言的指针一样操作内存空间的能力,过度的使用Unsafe类会使得出错的几率变大，因此Java官方并不建议使用的，官方文档也几乎没有。Oracle正在计划从Java 9中去掉Unsafe类.
- 该类采用了单例模式(它的单例,定义了私有静态变量,并在静态代码块中初始化,私有化构造方法),并且,如果直接调用,会抛出异常.
```
    public static Unsafe getUnsafe() {
	    //反射获取调用该方法的类的Class对象. 自己测试了下,获取到的是 java.lang.invoke.MethodHandleStatics ,而非真实的调用该方法的类的Class
        Class var0 = Reflection.getCallerClass();
        //判断var0的getClassLoader()结果是否为空,为空则抛出异常.
        if (!VM.isSystemDomainLoader(var0.getClassLoader())) {
            throw new SecurityException("Unsafe");
        } else {
            return theUnsafe;
        }
    }
```