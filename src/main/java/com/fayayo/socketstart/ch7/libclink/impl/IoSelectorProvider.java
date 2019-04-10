package com.fayayo.socketstart.ch7.libclink.impl;

import com.fayayo.socketstart.ch7.libclink.core.IoProvider;
import com.fayayo.socketstart.ch7.libclink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dalizu on 2018/11/26.
 * @version v1.0
 * @desc
 */
public class IoSelectorProvider implements IoProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    // 是否处于某个过程
    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);

    private final Selector readSelector;

    private final Selector writerSelector;

    private final ExecutorService inputHandlePool;

    private final ExecutorService outputHandlePool;

    private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();

    public IoSelectorProvider() throws IOException {

        readSelector = Selector.open();
        writerSelector = Selector.open();

        inputHandlePool = Executors.newFixedThreadPool(4, new IoProviderThreadFactory("IoProvider-Input-Thread-"));
        outputHandlePool = Executors.newFixedThreadPool(4, new IoProviderThreadFactory("IoProvider-Output-Thread-"));
        // 开始输出输入的监听
        startRead();
        startWrite();
    }

    private void startRead() {
        //独立一个线程
        Thread thread = new Thread("Clink IoSelectorProvider ReadSelector Thread") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        if (readSelector.select() == 0) {
                            //判断是否在注册,等待注册监听完成
                            waitSelection(inRegInput);
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for (SelectionKey key : selectionKeys) {
                            if (key.isValid()) {
                                //处理selector  重点
                                handleSelection(key, SelectionKey.OP_READ, inputCallbackMap, inputHandlePool);
                            }
                        }
                        selectionKeys.clear();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);//最高
        thread.start();//启动
    }


    private void startWrite() {
        //独立一个线程
        Thread thread = new Thread("Clink IoSelectorProvider WriterSelector Thread") {
            @Override
            public void run() {
                while (!isClosed.get()) {

                    try {
                        if (writerSelector.select() == 0) {
                            waitSelection(inRegOutput);
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = writerSelector.selectedKeys();
                        for (SelectionKey key : selectionKeys) {
                            if (key.isValid()) {
                                //处理selector  重点
                                handleSelection(key, SelectionKey.OP_WRITE, outputCallbackMap, outputHandlePool);
                            }
                        }
                        selectionKeys.clear();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);//最高
        thread.start();//启动
    }


    @Override
    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {
        //channel.register(readSelector,SelectionKey.OP_READ);

        return registerSelection(channel, readSelector, SelectionKey.OP_READ, inRegInput, inputCallbackMap, callback) != null;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {

        return registerSelection(channel, writerSelector, SelectionKey.OP_WRITE, inRegOutput, outputCallbackMap, callback) != null;
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {

        unRegisterSelection(channel, readSelector, inputCallbackMap);
    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel, writerSelector, outputCallbackMap);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            inputHandlePool.shutdown();
            outputHandlePool.shutdown();

            inputCallbackMap.clear();
            outputCallbackMap.clear();

            readSelector.wakeup();
            writerSelector.wakeup();

            CloseUtils.close(readSelector, writerSelector);
        }
    }


    private static void waitSelection(final AtomicBoolean locker) {

        synchronized (locker) {
            if (locker.get()) {
                try {
                    locker.wait();//如果被锁住了，这里就等待注册完成才进行下一个的selector
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }


    private static SelectionKey registerSelection(SocketChannel channel, Selector selector,
                                                  int registerOps, AtomicBoolean locker,
                                                  HashMap<SelectionKey, Runnable> map,
                                                  Runnable runnable) {
        synchronized (locker) {
            //设置锁定状态
            locker.set(true);

            try {
                // 唤醒当前的selector，让selector不处于select()状态,否则会是阻塞状态..
                selector.wakeup();

                SelectionKey key = null;
                if (channel.isRegistered()) {
                    // 查询是否已经注册过
                    key = channel.keyFor(selector);
                    if (key != null) {
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }

                if (key == null) {
                    // 注册selector得到Key
                    key = channel.register(selector, registerOps);
                    // 注册回调
                    map.put(key, runnable);
                }

                return key;

            } catch (Exception e) {

                return null;
            } finally {
                //解除锁定状态
                locker.set(false);
                try {
                    // 通知
                    locker.notify();
                } catch (Exception ignored) {
                }
            }

        }

    }

    private static void unRegisterSelection(SocketChannel channel, Selector selector,
                                            Map<SelectionKey, Runnable> map) {

        if (channel.isRegistered()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                // 取消监听的方法
                key.cancel();
                map.remove(key);
                selector.wakeup();//通知继续下一次selector操作
            }
        }
    }


    //执行过程
    private void handleSelection(SelectionKey key, int keyOps, HashMap<SelectionKey, Runnable> callbackMap,
                                 ExecutorService pool) {
        // 重点
        // 取消继续对keyOps的监听,当第一个连接可读后就拿出来，处理，后面就不用继续监听这个了，处理完成后，会加回去。
        key.interestOps(key.readyOps() & ~keyOps);

        Runnable runnable = null;

        try {
            runnable = callbackMap.get(key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (runnable != null && !pool.isShutdown()) {
            // 异步调度
            pool.execute(runnable);
        }

    }


    static class IoProviderThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IoProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }


}
