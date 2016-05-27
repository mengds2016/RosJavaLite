package org.ros.internal.transport.queue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.WritePendingException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.concurrent.CancellableLoop;
import org.ros.concurrent.CircularBlockingDeque;
import org.ros.internal.message.Message;
import org.ros.internal.message.MessageBufferPool;
import org.ros.internal.message.MessageBuffers;
import org.ros.internal.system.Utility;
import org.ros.internal.transport.ChannelHandlerContext;

/**
 * The outgoing message queue of a publisher processing type T messages.
 * A writer will be spun up in the executor that processes a deque with message entries,
 * serializing them to a bytebuffer for outbound asynch channel transport.
 * Each publisher created generates this writer which addresses all channels in the context array
 * it was created with.
 * @author jg
 */
public class OutgoingMessageQueue<T> {

  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(OutgoingMessageQueue.class);

  private static final int DEQUE_CAPACITY = 16384;

  private final CircularBlockingDeque<T> deque;
  private final Writer writer;
  private final MessageBufferPool messageBufferPool;
  private final ByteBuffer latchedBuffer;
  private final Object mutex;

  private boolean latchMode;
  private T latchedMessage;
  
  private List<ChannelHandlerContext> channels;
  /**
   * This class is submitted to the executor to process the deque entries
   * and serialize them to the latched buffer for outbound publisher channel
   * @author jg
   *
   */
  private final class Writer extends CancellableLoop {
	//final Object waitFinish = new Object();
    @Override
    public void loop() throws InterruptedException {
      T message = deque.takeFirst();
      final ByteBuffer buffer = (ByteBuffer) latchedBuffer.clear();//messageBufferPool.acquire();
      //messageBufferPool.release(buffer);
      //latchedBuffer.clear();
      Utility.serialize(message, buffer);
      //if(DEBUG) {
      //  log.info(String.format("Writing %d bytes.", buffer.position()));
      //}
      final Iterator<ChannelHandlerContext> it = channels.iterator();
      while(it.hasNext()) {
    	  final ChannelHandlerContext ctx = it.next();
    	  final CountDownLatch cdl = new CountDownLatch(1);
    	  //final Object waitFinish = ctx.getChannelCompletionMutex();
    	  boolean sendMessage;
    	  synchronized( ctx.getMessageTypes() ) {
    		  sendMessage = ctx.getMessageTypes().contains(message.getClass().getName().replace('.', '/'));
    	  }
    	  if( ctx.isReady() && sendMessage ) {
    		buffer.position(0);
    		ctx.write(buffer, new CompletionHandler<Integer, Void>() {
    				  @Override
    				  public void completed(Integer arg0, Void arg1) {
    					  if(DEBUG)
    						  log.info("sent "+buffer+" result:"+arg0+","+arg1);
    					  cdl.countDown();
    				  //messageBufferPool.release(buffer);
    				  }
    				  @Override
    				  public void failed(Throwable arg0, Void arg1) {
    					  // Broken pipe here if we cant write to client as its dropped
    					  //throw new RuntimeException(arg0);
    					  log.info("Closing failed write context:"+ctx+" due to "+arg0);
    					  arg0.printStackTrace();
    					  try {
    						  ctx.close();
    					  } catch (IOException e) {}
    					  // A write failure should render the context not ready for writes.
    					  ctx.setReady(false);
    					  cdl.countDown();
    				  }  
    		}); // completion handler
    		cdl.await();
    	  } // context ready
      } // ChannelhandlerContext iterator
    } // loop method
  }

  public OutgoingMessageQueue(ExecutorService executorService, List<ChannelHandlerContext> ctxs) throws IOException {
    deque = new CircularBlockingDeque<T>(DEQUE_CAPACITY);
    writer = new Writer();
    messageBufferPool = new MessageBufferPool();
    latchedBuffer = MessageBuffers.dynamicBuffer();
    mutex = new Object();
    latchMode = false;
    channels = ctxs;
    executorService.execute(writer);
  }

  public void setLatchMode(boolean enabled) {
    latchMode = enabled;
  }

  public boolean getLatchMode() {
    return latchMode;
  }

  /**
   * @param message
   *          the message to add to the queue
   */
  public void add(T message) {
    deque.addLast(message);
    setLatchedMessage(message);
  }

  private void setLatchedMessage(T message) {
    synchronized (mutex) {
      latchedMessage = message;
    }
  }

  /**
   * Stop writing messages and close all outgoing connections.
   */
  public void shutdown() {
    writer.cancel();
  }


  private void writeLatchedMessage() {
    synchronized (mutex) {
      latchedBuffer.clear();
      Utility.serialize(latchedMessage, latchedBuffer);
      Iterator<ChannelHandlerContext> it = channels.iterator();
      while(it.hasNext()) {
    	  ChannelHandlerContext ctx = it.next();
    	  ctx.write(latchedBuffer);
      }
    }
  }

  /**
   * @return the number of {@link Channel}s which have been added to this queue
   */
  public int getNumberOfChannels() {
    return channels.size();
  }

}
