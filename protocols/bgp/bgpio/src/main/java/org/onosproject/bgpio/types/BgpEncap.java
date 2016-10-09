/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.bgpio.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.util.Constants;
import com.google.common.base.MoreObjects;

/**
 * Implementation of BgpEncap.
 */
public class BgpEncap implements BgpValueType {

    public static final short TYPE = Constants.BGP_ENCAP;
    private int spec;
    private short tunnelType;

    public enum TunnelType {
        VXLAN((byte) 8);

        byte value;

        /**
         * Assign val with the value as the tunnel type.
         *
         * @param val tunnel type
         */
        TunnelType(byte val) {
            value = val;
        }

        /**
         * Returns value of route type.
         *
         * @return route type
         */
        public byte getType() {
            return value;
        }
    }

    /**
     * Resets fields.
     */
    public BgpEncap() {
        this.spec = 0;
        this.tunnelType = TunnelType.VXLAN.getType();
    }

    /**
     * Constructor to initialize parameters.
     *
     * @param routeTarget route target
     */
    public BgpEncap(int spec, short tunnelType) {
        this.spec = spec;
        this.tunnelType = tunnelType;
    }

    /**
     * Reads encapsulation from channelBuffer.
     *
     * @param cb channelBuffer
     * @return object of BgpEncap
     */
    public static BgpEncap read(ChannelBuffer cb) {
        return new BgpEncap(cb.readInt(), cb.readShort());
    }

    /**
     * Returns encapsulation spec.
     *
     * @return encapsulation spec
     */
    public int getSpec() {
        return this.spec;
    }

    /**
     * Returns encapsulation tunnel type.
     *
     * @return encapsulation tunnel type
     */
    public short getTunnelType() {
        return this.tunnelType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpEncap) {
            BgpEncap that = (BgpEncap) obj;
            if (this.spec == that.spec && this.tunnelType == that.tunnelType) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        // return Objects.hashCode(spec);
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("spec", spec)
                .add("tunnelType", tunnelType).toString();
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeShort(TYPE);
        cb.writeInt(spec);
        cb.writeShort(tunnelType);
        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public int compareTo(Object rd) {
        return 0;
    }
}
