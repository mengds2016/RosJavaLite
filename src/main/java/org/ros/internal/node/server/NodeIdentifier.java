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

package org.ros.internal.node.server;

import org.ros.internal.transport.ConnectionHeader;
import org.ros.internal.transport.ConnectionHeaderFields;
import org.ros.namespace.GraphName;
import org.ros.node.Node;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * A node slave identifier which combines the node name of a node with the Address
 * for contacting the node's RPC server.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class NodeIdentifier implements Serializable {
  private static final long serialVersionUID = -3493232406156967129L;
  
  private GraphName name;
  private InetSocketAddress uri;

  public static NodeIdentifier forName(String name) {
    return new NodeIdentifier(GraphName.of(name), null);
  }

  public static NodeIdentifier forUri(String uri, int port) {
    return new NodeIdentifier(null, new InetSocketAddress(uri, port));
  }

  public static NodeIdentifier forNameAndUri(String name, String uri, int port) {
    return new NodeIdentifier(GraphName.of(name), new InetSocketAddress(uri, port));
  }

  public NodeIdentifier() {}
  /**
   * Constructs a new {@link NodeIdentifier}.
   * 
   * Note that either {@code nodeName} or {@code uri} may be null but not both.
   * This is necessary because either is enough to uniquely identify a
   * {@link SlaveServer} and because, depending on context, one or the other may
   * not be available.
   * 
   * Although either value may be {@code null}, we do not treat {@code null} as
   * a wildcard with respect to equality. Even though it should be safe to do
   * so, wildcards are unnecessary in this case and would likely lead to buggy
   * code.
   * 
   * @param name
   *          the {@link GraphName} that the {@link Node} is known as
   * @param uri2
   *          the {@link URI} of the {@link Node}'s {@link SlaveServer} XML-RPC server
   */
  public NodeIdentifier(GraphName name, InetSocketAddress uri2) {
    assert(name != null || uri2 != null);
    if (name != null) {
      assert(name.isGlobal());
    }
    this.name = name;
    this.uri = uri2;
  }

  public GraphName getName() {
    return name;
  }

  public InetSocketAddress getUri() {
    return uri;
  }

  public ConnectionHeader toConnectionHeader() {
    ConnectionHeader connectionHeader = new ConnectionHeader();
    connectionHeader.addField(ConnectionHeaderFields.CALLER_ID, name.toString());
    return connectionHeader;
  }

  @Override
  public String toString() {
    return "NodeIdentifier<" + name + ", " + uri + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((uri == null) ? 0 : uri.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    NodeIdentifier other = (NodeIdentifier) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else { // name != null
    	if( other.name == null )
    		return false;
    	if (!name.equals(other.name))
    		return false;
    }
    if (uri == null) {
      if (other.uri != null)
        return false;
    } else { // uri != null
    	if( other.uri == null )
    		return false;
    	if (!uri.equals(other.uri))
    		return false;
    }
    return true;
  }
}
