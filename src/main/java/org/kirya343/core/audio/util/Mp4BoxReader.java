package org.kirya343.core.audio.util;

import java.nio.charset.StandardCharsets;

public class Mp4BoxReader {
    
    public record Mp4Box(
        String type,
        int size,
        int headerSize
    ) {
    }

    public static Mp4Box read(byte[] data, int offset) {
        if (offset + 8 > data.length) {
            return null;
        }

        long size = readUInt32(data, offset);

        String type = new String(
            data,
            offset + 4,
            4,
            StandardCharsets.US_ASCII
        );

        int headerSize = 8;

        if (size == 1) {
            if (offset + 16 > data.length) {
                return null;
            }

            size = readUInt64(data, offset + 8);
            headerSize = 16;
        } else if (size == 0) {
            return null;
        }

        if (size < headerSize || size > Integer.MAX_VALUE) {
            return null;
        }

        if (offset + size > data.length) {
            return null;
        }

        return new Mp4Box(
            type,
            (int) size,
            headerSize
        );
    }

    public static long readUInt32(byte[] data, int offset) {
        return ((long) (data[offset] & 0xff) << 24)
            | ((long) (data[offset + 1] & 0xff) << 16)
            | ((long) (data[offset + 2] & 0xff) << 8)
            | (data[offset + 3] & 0xffL);
    }

    public static long readUInt64(byte[] data, int offset) {
        return ((long) (data[offset] & 0xff) << 56)
            | ((long) (data[offset + 1] & 0xff) << 48)
            | ((long) (data[offset + 2] & 0xff) << 40)
            | ((long) (data[offset + 3] & 0xff) << 32)
            | ((long) (data[offset + 4] & 0xff) << 24)
            | ((long) (data[offset + 5] & 0xff) << 16)
            | ((long) (data[offset + 6] & 0xff) << 8)
            | (data[offset + 7] & 0xffL);
    }
}
