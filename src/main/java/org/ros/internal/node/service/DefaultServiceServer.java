package org.ros.internal.node.service;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.ChannelHandler;
import org.ros.address.AdvertiseAddress;
import org.ros.concurrent.ListenerGroup;
import org.ros.concurrent.SignalRunnable;
import org.ros.internal.message.MessageBuffers;
import org.ros.internal.message.service.ServiceDescription;
import org.ros.internal.node.topic.DefaultPublisher;
import org.ros.internal.system.Utility;
import org.ros.internal.transport.ChannelHandler;
import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.node.service.DefaultServiceServerListener;
import org.ros.node.service.ServiceResponseBuilder;
import org.ros.node.service.ServiceServer;
import org.ros.node.service.ServiceServerListener;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Default implementation of a {@link ServiceServer}.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class DefaultServiceServer<T, S> implements ServiceServer<T, S> {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(DefaultPublisher.class);

  private final ServiceDeclaration serviceDeclaration;
  private final ServiceResponseBuilder<T, S> serviceResponseBuilder;
  private final AdvertiseAddress advertiseAddress;
 
  private final MessageFactory messageFactory;
  private final ScheduledExecutorService scheduledExecutorService;
  private final ListenerGroup<ServiceServerListener<T, S>> listenerGroup;

  public DefaultServiceServer(ServiceDeclaration serviceDeclaration,
      ServiceResponseBuilder<T, S> serviceResponseBuilder, AdvertiseAddress advertiseAddress,
      MessageFactory messageFactory, ScheduledExecutorService scheduledExecutorService) {
    this.serviceDeclaration = serviceDeclaration;
    this.serviceResponseBuilder = serviceResponseBuilder;
    this.advertiseAddress = advertiseAddress;
    this.messageFactory = messageFactory;
    this.scheduledExecutorService = scheduledExecutorService;
    listenerGroup = new ListenerGroup<ServiceServerListener<T, S>>(scheduledExecutorService);
    listenerGroup.add(new DefaultServiceServerListener<T, S>() {
      @Override
      public void onMasterRegistrationSuccess(ServiceServer<T, S> registrant) {
        log.info("Service registered: " + DefaultServiceServer.this);
      }

      @Override
      public void onMasterRegistrationFailure(ServiceServer<T, S> registrant) {
        log.info("Service registration failed: " + DefaultServiceServer.this);
      }

      @Override
      public void onMasterUnregistrationSuccess(ServiceServer<T, S> registrant) {
        log.info("Service unregistered: " + DefaultServiceServer.this);
      }

      @Override
      public void onMasterUnregistrationFailure(ServiceServer<T, S> registrant) {
        log.info("Service unregistration failed: " + DefaultServiceServer.this);
      }
    });
  }

  public ByteBuffer finishHandshake(ConnectionHeader incomingConnectionHeader) {
    if (DEBUG) {
      log.info("Client handshake header: " + incomingConnectionHeader);
    }
    ConnectionHeader connectionHeader = toDeclaration().toConnectionHeader();
    String expectedChecksum = connectionHeader.getField(ConnectionHeaderFields.MD5_CHECKSUM);
    String incomingChecksum =
        incomingConnectionHeader.getField(ConnectionHeaderFields.MD5_CHECKSUM);
    // TODO(damonkohler): Pull out header field comparison logic.
    assert(incomingChecksum.equals(expectedChecksum) || incomingChecksum.equals("*"));
    if (DEBUG) {
      log.info("Server handshake header: " + connectionHeader);
    }
    ByteBuffer headbuf = MessageBuffers.dynamicBuffer();
    Utility.serialize(connectionHeader, headbuf);
    return headbuf;
  }

  @Override
  public InetSocketAddress getUri() {
    return advertiseAddress.toInetSocketAddress();//.toUri("rosrpc");
  }

  @Override
  public GraphName getName() {
    return serviceDeclaration.getName();
  }

  /**
   * @return a new {@link ServiceDeclaration} with this
   *         {@link DefaultServiceServer}'s {@link URI}
   */
  ServiceDeclaration toDeclaration() {
    ServiceIdentifier identifier = new ServiceIdentifier(serviceDeclaration.getName(), getUri());
    return new ServiceDeclaration(identifier, new ServiceDescription(serviceDeclaration.getType(),
        serviceDeclaration.getDefinition(), serviceDeclaration.getMd5Checksum()));
  }

  public ChannelHandler newRequestHandler() {
    return new ServiceRequestHandler<T, S>(serviceDeclaration, serviceResponseBuilder, messageFactory, scheduledExecutorService);
  }

  /**
   * Signal all {@link ServiceServerListener}s that the {@link ServiceServer}
   * has been successfully registered with the master.
   * 
   * <p>
   * Each listener is called in a separate thread.
   */
  public void signalOnMasterRegistrationSuccess() {
    final ServiceServer<T, S> serviceServer = this;
    listenerGroup.signal(new SignalRunnable<ServiceServerListener<T, S>>() {
      @Override
      public void run(ServiceServerListener<T, S> listener) {
        listener.onMasterRegistrationSuccess(serviceServer);
      }
    });
  }

  /**
   * Signal all {@link ServiceServerListener}s that the {@link ServiceServer}
   * has failed to register with the master.
   * 
   * <p>
   * Each listener is called in a separate thread.
   */
  public void signalOnMasterRegistrationFailure() {
    final ServiceServer<T, S> serviceServer = this;
    listenerGroup.signal(new SignalRunnable<ServiceServerListener<T, S>>() {
      @Override
      public void run(ServiceServerListener<T, S> listener) {
        listener.onMasterRegistrationFailure(serviceServer);
      }
    });
  }

  /**
   * Signal all {@link ServiceServerListener}s that the {@link ServiceServer}
   * has been successfully unregistered with the master.
   * 
   * <p>
   * Each listener is called in a separate thread.
   */
  public void signalOnMasterUnregistrationSuccess() {
    final ServiceServer<T, S> serviceServer = this;
    listenerGroup.signal(new SignalRunnable<ServiceServerListener<T, S>>() {
      @Override
      public void run(ServiceServerListener<T, S> listener) {
        listener.onMasterUnregistrationSuccess(serviceServer);
      }
    });
  }

  /**
   * Signal all {@link ServiceServerListener}s that the {@link ServiceServer}
   * has failed to unregister with the master.
   * 
   * <p>
   * Each listener is called in a separate thread.
   */
  public void signalOnMasterUnregistrationFailure() {
    final ServiceServer<T, S> serviceServer = this;
    listenerGroup.signal(new SignalRunnable<ServiceServerListener<T, S>>() {
      @Override
      public void run(ServiceServerListener<T, S> listener) {
        listener.onMasterUnregistrationFailure(serviceServer);
      }
    });
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addListener(ServiceServerListener<T, S> listener) {
    listenerGroup.add(listener);
  }

  @Override
  public String toString() {
    return "ServiceServer<" + toDeclaration() + ">";
  }
}
