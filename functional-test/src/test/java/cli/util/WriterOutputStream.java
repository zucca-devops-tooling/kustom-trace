package cli.util;


import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

// Helper for capturing System.out/err.
public class WriterOutputStream extends OutputStream {
    private final Writer writer;
    private final String encoding;
    public WriterOutputStream(Writer writer, String encoding) { this.writer = writer; this.encoding = encoding; }
    @Override public void write(byte[] b, int off, int len) throws IOException { writer.write(new String(b, off, len, encoding)); writer.flush(); }
    @Override public void write(int b) throws IOException { write(new byte[]{(byte)b}, 0, 1); }
    @Override public void flush() throws IOException { writer.flush(); }
    @Override public void close() throws IOException { writer.close(); }
}