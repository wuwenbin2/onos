package org.onosproject.bgpio.types;

import java.util.Objects;
import org.jboss.netty.buffer.ChannelBuffer;
import com.google.common.base.MoreObjects;

public class MplsLabel implements Comparable<MplsLabel> {

    public static final int MPLS_LABEL_LENGTH = 3;
    private byte[] mplsLabel;

    /**
     * Resets fields.
     */
    public MplsLabel() {
        this.mplsLabel = null;
    }

    /**
     * Constructor to initialize parameters.
     *
     * @param mplsLabel mpls label
     */
    public MplsLabel(byte[] mplslabel) {
        this.mplsLabel = mplslabel;
    }

    /**
     * Reads mpls label from channelBuffer.
     *
     * @param cb channelBuffer
     * @return object of mpls label
     */
    public static MplsLabel read(ChannelBuffer cb) {
        return new MplsLabel(cb.readBytes(3).array());
    }

    /**
     * writes mpls label into channelBuffer.
     *
     * @param cb channelBuffer
     * @return length length of written data
     */
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeBytes(mplsLabel);
        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Returns mpls label.
     *
     * @return mpls label
     */
    public byte[] getMplsLabel() {
        return this.mplsLabel;
    }

    @Override
    public int compareTo(MplsLabel mplsLabel) {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof MplsLabel) {

            MplsLabel that = (MplsLabel) obj;

            if (this.mplsLabel == that.mplsLabel) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mplsLabel);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("mplsLabel", mplsLabel).toString();
    }
}
