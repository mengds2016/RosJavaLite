/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.address;

import org.ros.exception.RosRuntimeException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class to generate addresses for various servers
 * @author damonkohler@google.com (Damon Kohler)
 * @author jg
 */
public class InetSocketAddressFactory {
  private static AtomicInteger portSelector = new AtomicInteger(8090); // add and get by 2 each call, so first is 8090
  
  private InetSocketAddressFactory() {
    // Utility class
  }

  private static boolean isIpv4(InetSocketAddress address) {
    return address.getAddress().getAddress().length == 4;
  }
  private static boolean isIpv4(InetAddress address) {
	    return address.getAddress().length == 4;
  }
  
  private static Collection<InetAddress> getAllInetAddresses() {
    List<NetworkInterface> networkInterfaces;
    try {
      networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
    } catch (SocketException e) {
      throw new RosRuntimeException(e);
    }
    List<InetAddress> inetAddresses = new ArrayList<InetAddress>();
    for (NetworkInterface networkInterface : networkInterfaces) {
      inetAddresses.addAll(Collections.list(networkInterface.getInetAddresses()));
    }
    return inetAddresses;
  }

  public static InetAddress newNonLoopback() {
    for (InetAddress address : getAllInetAddresses()) {
      // IPv4 only for now.
      if (!address.isLoopbackAddress() && isIpv4(address)) {
        return address;
      }
    }
    throw new RosRuntimeException("No non-loopback interface found.");
  }

  private static Collection<InetAddress> getAllInetAddressByName(String host) {
    InetAddress[] allAddressesByName;
    try {
      allAddressesByName = InetAddress.getAllByName(host);
    } catch (UnknownHostException unused) {
      try {
        allAddressesByName = InetAddress.getAllByName(host);
      } catch (UnknownHostException e) {
        throw new RosRuntimeException(e);
      }
    }
    return Arrays.asList(allAddressesByName);
  }

  /**
   * Creates an {@link InetAddress} with both an IP and a host set so that no
   * further resolving will take place.
   * 
   * If an IP address string is specified, this method ensures that it will be
   * used in place of a host name.
   * 
   * If a host name other than {@code Address.LOCALHOST} is specified, this
   * method trys to find a non-loopback IP associated with the supplied host
   * name.
   * 
   * If the specified host name is {@code Address.LOCALHOST}, this method
   * returns a loopback address.
   * 
   * In all cases the port is incremented by 2 each new address for a master and parameter server port
   * combination;
   * 
   * @param host
   * @return an {@link InetAddress} with both an IP and a host set (no further resolving will take place)
   */
  public static InetSocketAddress newFromHostString(String host) {
    try {
      InetAddress target = null;
      if (InetAddress.getByName(host) != null) {
        return new InetSocketAddress(InetAddress.getByAddress(host, InetAddress.getByName(host).getAddress()), 
        		portSelector.addAndGet(2));
      }
      if (host.equals(Address.LOCALHOST)) {
        return new InetSocketAddress(InetAddress.getByAddress(Address.LOCALHOST, InetAddress.getByName(Address.LOOPBACK).getAddress()),
        		portSelector.addAndGet(2));
      }
    } catch (UnknownHostException e) {
      throw new RosRuntimeException(e);
    }
    Collection<InetAddress> allAddressesByName = getAllInetAddressByName(host);
    // First, try to find a non-loopback IPv4 address.
    for (InetAddress address : allAddressesByName) {
      if (!address.isLoopbackAddress() && isIpv4(address)) {
        return new InetSocketAddress(address, portSelector.addAndGet(2));
      }
    }
    // Return a loopback IPv4 address as a last resort.
    for (InetAddress address : allAddressesByName) {
      if (isIpv4(address)) {
        return new InetSocketAddress(address, portSelector.addAndGet(2));
      }
    }
    throw new RosRuntimeException("Unable to construct InetAddress for host: " + host);
  }

  public static InetSocketAddress newLoopback() {
    return newFromHostString(Address.LOOPBACK);
  }
  
  public static InetSocketAddress newLoopback(int port) {
	    return new InetSocketAddress(Address.LOOPBACK, port);
  }
}