/*
 * Automatically generated by jrpcgen 1.0.7 on 2/21/09 1:22 AM
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 */
package org.dcache.chimera.nfs.v4.xdr;
import java.util.Arrays;
import org.dcache.xdr.*;
import java.io.IOException;
import java.io.Serializable;

public class stateid4 implements XdrAble, Serializable {

    static final long serialVersionUID = -6677150504723505919L;

    public final static stateid4 CURRENT_STATEID =
            new stateid4(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 1);

    public final static stateid4 INVAL_STATEID =
            new stateid4(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, nfs4_prot.NFS4_UINT32_MAX);

    public uint32_t seqid;
    public byte [] other;

    public stateid4() {
    }

    private stateid4(byte[] other, int seq) {
        this.other = other;
        seqid = new uint32_t(seq);
    }

    public stateid4(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        seqid.xdrEncode(xdr);
        xdr.xdrEncodeOpaque(other, 12);
    }

    public void xdrDecode(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        seqid = new uint32_t(xdr);
        other = xdr.xdrDecodeOpaque(12);
    }

    @Override
    public boolean equals(Object obj) {

        if( obj == this) return true;
        if( !(obj instanceof stateid4) ) return false;

        final stateid4 other_id = (stateid4) obj;

        return Arrays.equals(this.other, other_id.other);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(other);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        for(byte b : other) {
            if(b < 0x10)
                sb.append("0");
            sb.append(Integer.toHexString(b));
        }

        sb.append(", seq: ").append(seqid.value).append("]");
        return sb.toString();
    }

}
// End of stateid4.java