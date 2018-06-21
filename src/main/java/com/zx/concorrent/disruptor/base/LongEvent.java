package com.zx.concorrent.disruptor.base;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 长整型元素对象
 * disruptor需要保存的单个数据对象
 */
@Data
class LongEvent {
    private long value;
}

/**
 * 长整型元素对象 工厂
 */
class LongEventFactory implements EventFactory<LongEvent>{
    @Override
    public LongEvent newInstance() {
        return new LongEvent();
    }
}

/**
 * 消费者
 */
@Slf4j
class LongEventHandler implements EventHandler<LongEvent> {

    /**
     * 发布者将event发布到{@link com.lmax.disruptor.RingBuffer}时调用，
     *
     * {@link com.lmax.disruptor.BatchEventProcessor}将批量读取{@link com.lmax.disruptor.RingBuffer}中的消息。
     *
     * 批处理可以处理所有event，不必等待任何新event到达。 这对于io操作这样的慢操作事件处理非常有用，因为可以将来自
     * 多个event的数据组合成单个操作。
     *
     * 实现应确保endOfBatch为true时，该消息始终在该消息和下个消息之间的时间被处理
     * @param longEvent 当前event
     * @param sequence 当前event的序列号
     * @param endOfBatch 表示该元素是否是来自{@link com.lmax.disruptor.RingBuffer}中该批次的最后一个元素
     * @throws Exception 如果要在链中进一步处理异常
     */
    @Override
    public void onEvent(LongEvent longEvent, long sequence, boolean endOfBatch) throws Exception {
        log.info("sequence:{},endOfBatch:{},longEvent:{}",sequence,endOfBatch,longEvent);
    }
}

/**
 * 生产者
 */
class LongEventProducer{

}