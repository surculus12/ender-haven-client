package wiki.client;

import java.io.IOException;
import java.io.InputStream;

public class Message {
    // uint16 as int because java is bad
    public final int type;
    public final int size;
    public final byte[] payload;

    public Message(int type, byte[] payload) {
        this.type = type;
        this.size = payload.length;
        this.payload = payload;
    }

    public byte[] encode() {
        byte[] b = new byte[6 + payload.length];
        b[0] = (byte) (type & 0xff);
        b[1] = (byte) ((type & 0xff00) >> 8);
        b[2] = (byte) (size & 0xff);
        b[3] = (byte) ((size & 0xff00) >> 8);
        b[4] = (byte) ((size & 0xff0000) >> 16);
        b[5] = (byte) ((size & 0xff000000) >> 24);
        System.arraycopy(payload, 0, b, 6, payload.length);
        return b;
    }

    public static Message read(InputStream in) throws IOException {
        byte[] bType = new byte[2];
        int n = in.read(bType);
        if (n != 2) {
            throw new IOException("Type header not fully read");
        }
        int type = decodeTypeHeader(bType);

        byte[] bSize = new byte[4];
        n = in.read(bSize);
        if (n != 4) {
            throw new IOException("Size header not fully read");
        }
        int size = decodeSizeHeader(bSize);

        byte[] payload = new byte[size];
        n = in.read(payload);
        if (n != size) {
            throw new IOException("Payload not fully read");
        }
        return new Message(type, payload);
    }

    public static int decodeTypeHeader(byte[] b) {
        return (b[0]) & 0xff |
                (b[1] << 8) & 0xff00;
    }

    public static int decodeSizeHeader(byte[] b) {
        return (b[0]) & 0xff |
                (b[1] << 8) & 0xff00 |
                (b[2] << 16) & 0xff0000 |
                (b[3] << 24) & 0xff000000;
    }
}
