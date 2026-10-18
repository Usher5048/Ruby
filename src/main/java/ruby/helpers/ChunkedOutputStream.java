package ruby.helpers;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayOutputStream;
import java.util.*;

public class ChunkedOutputStream extends ByteArrayOutputStream {
    private final List<byte[]> chunks = new ArrayList<>();

    public void prepend(int b) {
        this.chunks.addFirst(new byte[] {(byte) b});
    }
    public void prepend(byte[] b) {
        this.prepend(b, 0, b.length);
    }
    public void prepend(byte[] b, int off, int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        this.chunks.addFirst(Arrays.copyOfRange(b, off, len));
    }

    public void insert(int idx, int b) {
        if(idx < 0) idx += this.chunks.size();
        this.chunks.add(idx, new byte[] {(byte) b});
    }

    public void insert(int idx, byte[] b) {
        this.insert(idx, b, 0, b.length);
    }
    public void insert(int idx, byte[] b, int off, int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        if(idx < 0) idx += this.chunks.size();
        this.chunks.add(idx, Arrays.copyOfRange(b, off, len));
    }

    @Override
    public int size() {
        int size = 0;
        for(byte[] chunk : this.chunks) size += chunk.length;
        return size;
    }

    @Override
    public byte @NotNull [] toByteArray() {
        int ptr = 0;
        byte[] fin = new byte[0];
        for(byte[] chunk : this.chunks) {
            fin = Arrays.copyOf(fin, fin.length + chunk.length);
            System.arraycopy(chunk, 0, fin, ptr, chunk.length);
            ptr += chunk.length;
        }

        return fin;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        this.chunks.addLast(Arrays.copyOfRange(b, off, len));
    }

    @Override
    public void writeBytes(byte @NonNull [] b) {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(int b) {
        this.chunks.addLast(new byte[] {(byte) b});
    }
}
