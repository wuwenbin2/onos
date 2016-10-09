package org.onosproject.bgpio.protocol.evpn;

import org.jboss.netty.buffer.ChannelBuffer;

public interface RouteTypeSpec {

    /**
     * Returns the Type of RouteTypeSpefic.
     *
     * @return short value of type
     */
    RouteType getType();

    /**
     * Writes the byte Stream of BGP Message to channel buffer.
     *
     * @param cb channel buffer
     * @return length written to channel buffer
     * @throws BgpParseException
     */
    int write(ChannelBuffer cb);

}
