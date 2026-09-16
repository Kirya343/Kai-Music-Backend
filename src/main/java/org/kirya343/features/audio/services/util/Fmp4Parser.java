package org.kirya343.features.audio.services.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kirya343.features.audio.dto.AudioChunk;

public class Fmp4Parser {

    public Result parse(byte[] data) throws IOException {
        List<AudioChunk> chunks = new ArrayList<>();
        List<Double> startTimes = new ArrayList<>();

        ByteArrayOutputStream initialization =
            new ByteArrayOutputStream();

        ByteArrayOutputStream currentFragment = null;

        boolean mediaStarted = false;

        long timescale = 0;
        long sequence = 0;
        double currentStartTime = 0;

        int offset = 0;

        while (offset < data.length) {
            Mp4BoxReader.Mp4Box box =
                Mp4BoxReader.read(data, offset);

            if (box == null) {
                throw new IOException(
                    "Invalid MP4 box at offset " + offset
                );
            }

            byte[] boxData = Arrays.copyOfRange(
                data,
                offset,
                offset + box.size()
            );

            switch (box.type()) {
                case "ftyp" -> {
                    initialization.write(boxData);
                }

                case "moov" -> {
                    initialization.write(boxData);

                    if (timescale == 0) {
                        timescale =
                            Mp4MetadataReader.readAudioTimescale(
                                boxData
                            );
                    }
                }

                case "moof" -> {
                    mediaStarted = true;

                    long decodeTime =
                        Mp4MetadataReader.readTfdt(boxData);

                    currentStartTime =
                        (double) decodeTime / timescale;

                    currentFragment =
                        new ByteArrayOutputStream();

                    currentFragment.write(boxData);
                }

                case "mdat" -> {
                    if (currentFragment == null) {
                        throw new IOException(
                            "mdat found without preceding moof"
                        );
                    }

                    currentFragment.write(boxData);

                    chunks.add(
                        new AudioChunk(
                            currentFragment.toByteArray(),
                            sequence++,
                            5000,
                            false
                        )
                    );

                    startTimes.add(currentStartTime);

                    currentFragment = null;
                }

                default -> {
                    if (!mediaStarted) {
                        initialization.write(boxData);
                    } else if (currentFragment != null) {
                        currentFragment.write(boxData);
                    }
                }
            }

            offset += box.size();
        }

        if (initialization.size() == 0) {
            throw new IOException(
                "Missing MP4 initialization segment"
            );
        }

        AudioChunk initializationChunk =
            new AudioChunk(
                initialization.toByteArray(),
                -1,
                0,
                true
            );

        return new Result(
            initializationChunk,
            chunks,
            startTimes
        );
    }
    
    public record Result(
        AudioChunk initializationChunk,
        List<AudioChunk> chunks,
        List<Double> startTimes
    ) {
    }
}
