package org.ros.internal.transport;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * A handler context contains all the executor, the channel group, the channel, and the pipeline with the handlers.
 * There is one channel per context.
 * There is one pipeline context.
 * There is one executor per group of contexts.
 * The pipeline is a stateful collection of handlers that represent the current channel state and means
 * of executing functions in the process of connecting, disconnecting, reading, failing etc.
 * The pipeline is configured by means of factories that create ChannelInitializers, inserting
 * them in order in the pipeline deque.
 * The functions of the system move data through the pipeline, triggering the handlers in the sequence they were
 * added.
 * @author jg
 *
 */
public class ChannelHandlerContextImpl implements ChannelHandlerContext {
	AsynchronousChannelGroup channelGroup;
	Executor executor;
	AsynchronousSocketChannel channel;
	ChannelPipeline pipeline;
	
	ByteBuffer buf;
	int MAXBUF = 2000000;
	
	public ChannelHandlerContextImpl(AsynchronousChannelGroup grp, AsynchronousSocketChannel ch, Executor exc) {
		channelGroup = grp;
		channel = ch;
		executor = exc;
		buf = ByteBuffer.allocate(MAXBUF);
		pipeline = new ChannelPipelineImpl(this);
	}
	
	public void setChannel(AsynchronousSocketChannel sock) {
		this.channel = sock;
	}
	
	
	@Override
	public Executor executor() {
		return executor;
	}

	@Override
	public String name() {
		return "ChannelHandlerContext";
	}


	@Override
	public AsynchronousSocketChannel bind(SocketAddress localAddress) throws IOException {
		return channel.bind(localAddress);
	}

	@Override
	public void connect(SocketAddress remoteAddress) {
		channel.connect(remoteAddress);
	}

	@Override
	public void connect(SocketAddress remoteAddress, SocketAddress localAddress) throws IOException {
		channel.bind(localAddress);
		channel.connect(remoteAddress);
	}


	@Override
	public Future<Void> disconnect() {
		throw new RuntimeException("nope..");
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}


	@Override
	public Future<Integer> read() {
		return channel.read(buf);	
	}

	@Override
	public Future<Integer> write(Object msg) {
		return channel.write((ByteBuffer) msg);
	}

	@Override
	public Future<Void> write(Object msg,
			CompletionHandler<Integer, Void> handler) {
		throw new RuntimeException("nope..");
	}

	@Override
	public ChannelHandlerContext flush() {
		throw new RuntimeException("nope..");
	}

	@Override
	public Future<Void> writeAndFlush(Object msg,
			CompletionHandler<Integer, Void> handler) {
		throw new RuntimeException("nope..");
	}

	@Override
	public Future<Integer> writeAndFlush(Object msg) {
		return channel.write((ByteBuffer) msg);
	}

	@Override
	public ChannelPipeline pipeline() {
		return pipeline;
	}

	@Override
	public AsynchronousChannelGroup getChannelGroup() {
		return channelGroup;
	}

	@Override
	public Channel channel() {
		return channel;
	}

	@Override
	public String toString() {
		return new String("ChannelHandlerContext:"+channel+" "+channelGroup+" "+executor+" "+pipeline);
	}

}
