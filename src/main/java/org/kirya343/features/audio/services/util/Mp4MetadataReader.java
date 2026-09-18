package org.kirya343.features.audio.services.util;

import java.io.IOException;
import java.util.Arrays;

import org.kirya343.features.audio.services.util.Mp4BoxReader.Mp4Box;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public class Mp4MetadataReader {
    
    public static long readAudioTimescale(byte[] moov) throws IOException {
        int offset = 0;

        while (offset < moov.length) {
            Mp4Box box = Mp4BoxReader.read(moov, offset);

            if (box == null) {
                throw new IOException(
                    "Invalid MP4 box while reading timescale"
                );
            }

            byte[] boxData = Arrays.copyOfRange(
                moov,
                offset,
                offset + box.size()
            );

            if ("mdhd".equals(box.type())) {
                return readMdhdTimescale(boxData);
            }

            if ("moov".equals(box.type())
                || "trak".equals(box.type())
                || "mdia".equals(box.type())) {

                long result = findTimescaleInChildren(boxData);

                if (result > 0) {
                    return result;
                }
            }

            offset += box.size();
        }

        throw new IOException("Audio timescale not found in moov");
    }

    private static long findTimescaleInChildren(byte[] data) throws IOException {
        Mp4Box root = Mp4BoxReader.read(data, 0);

        if (root == null) {
            throw new IOException("Invalid MP4 container");
        }

        int offset = root.headerSize();

        while (offset < data.length) {
            Mp4Box box = Mp4BoxReader.read(data, offset);

            if (box == null) {
                throw new IOException(
                    "Invalid MP4 box while searching for timescale"
                );
            }

            byte[] boxData = Arrays.copyOfRange(
                data,
                offset,
                offset + box.size()
            );

            if ("mdhd".equals(box.type())) {
                return readMdhdTimescale(boxData);
            }

            if ("trak".equals(box.type())
                || "mdia".equals(box.type())) {

                long result = findTimescaleInChildren(boxData);

                if (result > 0) {
                    return result;
                }
            }

            offset += box.size();
        }

        return 0;
    }

    private static long readMdhdTimescale(byte[] mdhd) throws IOException {
        int version = mdhd[8] & 0xff;

        int timescaleOffset;

        if (version == 0) {
            // mdhd:
            // 8  = version + flags
            // 12 = creation_time
            // 16 = modification_time
            // 20 = timescale
            timescaleOffset = 20;
        } else if (version == 1) {
            // mdhd:
            // 8  = version + flags
            // 12 = creation_time (8 bytes)
            // 20 = modification_time (8 bytes)
            // 28 = timescale
            timescaleOffset = 28;
        } else {
            throw new IOException(
                "Unsupported mdhd version: " + version
            );
        }

        if (mdhd.length < timescaleOffset + 4) {
            throw new IOException("Invalid mdhd box");
        }

        return Mp4BoxReader.readUInt32(mdhd, timescaleOffset);
    }

    public static long readTfdt(byte[] moof) throws IOException {
        Mp4Box root = Mp4BoxReader.read(moof, 0);

        if (root == null) {
            throw new IOException("Invalid MP4 moof");
        }

        int offset = root.headerSize();

        while (offset < moof.length) {
            Mp4Box box = Mp4BoxReader.read(moof, offset);

            if (box == null) {
                throw new IOException(
                    "Invalid MP4 box while reading tfdt"
                );
            }

            byte[] boxData = Arrays.copyOfRange(
                moof,
                offset,
                offset + box.size()
            );

            if ("tfdt".equals(box.type())) {
                return readTfdtValue(boxData);
            }

            if ("traf".equals(box.type())) {
                long result = findTfdtInTraf(boxData);

                if (result >= 0) {
                    return result;
                }
            }

            offset += box.size();
        }

        throw new IOException("tfdt not found in moof");
    }

    private static long findTfdtInTraf(byte[] traf) throws IOException {
        Mp4Box root = Mp4BoxReader.read(traf, 0);

        if (root == null) {
            throw new IOException("Invalid MP4 moof");
        }

        int offset = root.headerSize();

        while (offset < traf.length) {
            Mp4Box box = Mp4BoxReader.read(traf, offset);

            if (box == null) {
                throw new IOException(
                    "Invalid MP4 box while searching for tfdt"
                );
            }

            byte[] boxData = Arrays.copyOfRange(
                traf,
                offset,
                offset + box.size()
            );

            if ("tfdt".equals(box.type())) {
                return readTfdtValue(boxData);
            }

            offset += box.size();
        }

        return -1;
    }

    private static long readTfdtValue(byte[] tfdt) throws IOException {
        if (tfdt.length < 12) {
            throw new IOException("Invalid tfdt box");
        }

        int version = tfdt[8] & 0xff;

        if (version == 0) {
            return Mp4BoxReader.readUInt32(tfdt, 12);
        }

        if (version == 1) {
            return Mp4BoxReader.readUInt64(tfdt, 12);
        }

        throw new IOException(
            "Unsupported tfdt version: " + version
        );
    }

    public static long readAudioDuration(byte[] moov) throws IOException {
        int offset = Mp4BoxReader.read(moov, 0).headerSize();

        while (offset < moov.length) {
            Mp4BoxReader.Mp4Box trak = Mp4BoxReader.read(moov, offset);

            if (trak == null) {
                throw new IOException(
                    "Invalid MP4 box at offset " + offset
                );
            }

            if ("trak".equals(trak.type())) {
                byte[] trakData = Arrays.copyOfRange(
                    moov,
                    offset,
                    offset + trak.size()
                );

                Long duration = readAudioDurationFromTrak(trakData);

                if (duration != null) {
                    return duration;
                }
            }

            offset += trak.size();
        }

        throw new IOException("Audio track duration not found");
    }

    private static Long readAudioDurationFromTrak(byte[] trak) throws IOException {
        Mp4BoxReader.Mp4Box root = Mp4BoxReader.read(trak, 0);

        if (root == null) {
            throw new IOException("Invalid trak box");
        }

        int offset = root.headerSize();

        byte[] mdiaData = null;

        while (offset < trak.length) {
            Mp4BoxReader.Mp4Box box = Mp4BoxReader.read(trak, offset);

            if (box == null) {
                throw new IOException(
                    "Invalid MP4 box at offset " + offset
                );
            }

            if ("mdia".equals(box.type())) {
                mdiaData = Arrays.copyOfRange(
                    trak,
                    offset,
                    offset + box.size()
                );

                break;
            }

            offset += box.size();
        }

        if (mdiaData == null) {
            return null;
        }

        Mp4BoxReader.Mp4Box mdiaRoot = Mp4BoxReader.read(mdiaData, 0);
        int mdiaOffset = mdiaRoot.headerSize();

        long timescale = 0;
        long duration = 0;
        boolean audio = false;

        while (mdiaOffset < mdiaData.length) {
            Mp4BoxReader.Mp4Box box =
                Mp4BoxReader.read(mdiaData, mdiaOffset);

            if (box == null) {
                throw new IOException(
                    "Invalid MP4 box at offset " + mdiaOffset
                );
            }

            byte[] boxData = Arrays.copyOfRange(
                mdiaData,
                mdiaOffset,
                mdiaOffset + box.size()
            );

            switch (box.type()) {
                case "hdlr" -> {
                    if (boxData.length < box.headerSize() + 12) {
                        throw new IOException("Invalid hdlr box");
                    }

                    int contentOffset = box.headerSize();

                    int handlerTypeOffset = contentOffset + 8;

                    String handlerType = new String(
                        boxData,
                        handlerTypeOffset,
                        4,
                        java.nio.charset.StandardCharsets.ISO_8859_1
                    );

                    audio = "soun".equals(handlerType);
                }

                case "mdhd" -> {
                    int contentOffset = box.headerSize();

                    int version = boxData[contentOffset] & 0xFF;

                    if (version == 0) {
                        timescale = Mp4BoxReader.readUInt32(
                            boxData,
                            contentOffset + 12
                        );

                        duration = Mp4BoxReader.readUInt32(
                            boxData,
                            contentOffset + 16
                        );
                    } else if (version == 1) {
                        timescale = Mp4BoxReader.readUInt32(
                            boxData,
                            contentOffset + 20
                        );

                        duration = Mp4BoxReader.readUInt64(
                            boxData,
                            contentOffset + 24
                        );
                    } else {
                        throw new IOException(
                            "Unsupported mdhd version: " + version
                        );
                    }
                }
            }

            mdiaOffset += box.size();
        }

        if (!audio || timescale <= 0) {
            return null;
        }

        return duration * 1000L / timescale;
    }
}
