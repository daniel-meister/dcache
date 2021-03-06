/*
 * $Id: TunnelOutputStream.java,v 1.2 2006-10-11 07:54:50 tigran Exp $
 */

package javatunnel;

import java.io.IOException;
import java.io.OutputStream;

class TunnelOutputStream extends OutputStream
{
    private static final int ARRAYMAXLEN = 4096;

    private OutputStream _out;
    private Convertable _converter;
    private byte[] _buffer = new byte[ARRAYMAXLEN];
    private int _pos;

    public TunnelOutputStream(OutputStream out, Convertable converter)
    {
        _out = out;
        _converter = converter;
    }

    @Override
    public void write(int b) throws IOException
    {
        _buffer[_pos] = (byte)b;
        ++_pos;

        if(_pos >= ARRAYMAXLEN) {
            flush();
        }
    }

    @Override
    public void flush() throws IOException
    {
        _converter.encode( _buffer, _pos, _out);
        _pos = 0;
    }


    @Override
    public void close() throws IOException
    {
            _out.close();
    }
}
