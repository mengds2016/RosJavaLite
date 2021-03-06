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

package org.ros.internal.transport;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.ros.address.AdvertiseAddress;

/**
 * ProtocolDescription gets shipped around as serialized object passed to configure slaves that span TCP
 * processes to do the OOB data comm. In our RosJavaLite the Serializable wiring is in place here and in subclasses.
 * @author damonkohler@google.com (Damon Kohler)
 * @author jg
 */
public class ProtocolDescription implements Serializable {
  private static final long serialVersionUID = -714032869496198686L;
  private String name;
  private AdvertiseAddress address;

  public ProtocolDescription(String name, AdvertiseAddress address) {
    this.name = name;
    this.address = address;
  }

  public ProtocolDescription() {}

  public String getName() {
    return name;
  }

  public AdvertiseAddress getAdverstiseAddress() {
    return address;
  }
  
  public InetSocketAddress getAddress() {
    return address.toInetSocketAddress();
  }

  public List<Object> toList() {
    ArrayList<Object> l1 = new ArrayList<Object>();
    l1.add((Object) name);
    l1.add(address.getHost());
    l1.add( address.getPort());
    return l1;
  }

  @Override
  public String toString() {
    return "Protocol<" + name + ", " + getAdverstiseAddress() + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((address == null) ? 0 : address.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
    ProtocolDescription other = (ProtocolDescription) obj;
    if (address == null) {
      if (other.address != null)
        return false;
    } else if (!address.equals(other.address))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

}
