package org.kirya343.features.audio.services.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kirya343.features.audio.dto.AudioChunk;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public class Fmp4Parser {

    private AudioChunk initializationChunk;
    private List<AudioChunk> chunks = new ArrayList<>();
    private List<Double> startTimes = new ArrayList<>();
    private long durationMs = 0;
    private byte[] data;

    public Fmp4Parser(Path file) throws IOException {
        this.data = Fmp4Encoder.encode(file);

        parse();
    }

    public void parse() throws IOException {

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

            //log.debug("Chunk {}, type: {}", sequence, box.type());

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

                    if (durationMs == 0) {
                        durationMs =
                            Mp4MetadataReader.readAudioDuration(boxData);

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

                    chunks.add(new AudioChunk(
                        currentFragment.toByteArray(),
                        sequence++,
                        5000,
                        false
                    ));

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

        initializationChunk = new AudioChunk(
            initialization.toByteArray(),
            -1,
            0,
            true
        );

        log.info("данные парсера заполнены");
    }
    
    public AudioChunk getInitializationChunk() {
        return initializationChunk;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public List<AudioChunk> getAudioChunks() {
        return chunks;
    }

    public List<Double> getStartTimes() {
        return startTimes;
    }
}
