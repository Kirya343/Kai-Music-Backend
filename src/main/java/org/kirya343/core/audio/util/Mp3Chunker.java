package org.kirya343.core.audio.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.kirya343.dto.audio.AudioChunk;

public class Mp3Chunker {

    private static final int TARGET_DURATION_MS = 5000;

    private final byte[] audio;
    private int offset = 0;
    private int byteOffset = 0;
    private long sequence = 0;

    public Mp3Chunker(Path file) throws IOException {
        this.audio = Files.readAllBytes(file);
    }

    public AudioChunk nextChunk() {
        if (byteOffset >= audio.length) {
            return null;
        }

        int chunkStart = byteOffset;
        int durationMs = 0;

        while (byteOffset < audio.length && durationMs < TARGET_DURATION_MS) {

            Mp3Frame frame = readFrame(byteOffset);

            if (frame == null) {
                byteOffset++;
                continue;
            }

            byteOffset += frame.frameLength;
            durationMs += frame.durationMs;
        }

        AudioChunk chunk = new AudioChunk(
            Arrays.copyOfRange(audio, chunkStart, byteOffset),
            sequence++,
            chunkStart,
            durationMs
        );

        return chunk;
    }

    public boolean hasNext() {
        return offset < audio.length;
    }

    private Mp3Frame readFrame(int pos) {

        if (pos + 4 > audio.length) {
            return null;
        }

        int b1 = audio[pos] & 0xFF;
        int b2 = audio[pos + 1] & 0xFF;
        int b3 = audio[pos + 2] & 0xFF;
        int b4 = audio[pos + 3] & 0xFF;

        // Sync word: 11 бит должны быть единицами
        if (b1 != 0xFF || (b2 & 0xE0) != 0xE0) {
            return null;
        }

        int version = (b2 >> 3) & 0x03;
        int layer = (b2 >> 1) & 0x03;
        int bitrateIndex = (b3 >> 4) & 0x0F;
        int sampleRateIndex = (b3 >> 2) & 0x03;
        int padding = (b3 >> 1) & 0x01;

        if (layer != 1) {
            return null; // Только Layer III (MP3)
        }

        if (bitrateIndex == 0 || bitrateIndex == 15) {
            return null;
        }

        if (sampleRateIndex == 3) {
            return null;
        }

        int sampleRate = getSampleRate(version, sampleRateIndex);

        int bitrate = getBitrate(version, bitrateIndex);

        if (sampleRate <= 0 || bitrate <= 0) {
            return null;
        }

        int samplesPerFrame;

        if (version == 3) {
            // MPEG 1
            samplesPerFrame = 1152;
        } else {
            // MPEG 2 / 2.5
            samplesPerFrame = 576;
        }

        int frameLength;

        if (version == 3) {
            frameLength =
                    (144 * bitrate * 1000) / sampleRate + padding;
        } else {
            frameLength =
                    (72 * bitrate * 1000) / sampleRate + padding;
        }

        if (frameLength <= 0 || pos + frameLength > audio.length) {
            return null;
        }

        int durationMs =
                (samplesPerFrame * 1000) / sampleRate;

        return new Mp3Frame(frameLength, durationMs);
    }

    private int getSampleRate(int version, int index) {

        int[][] sampleRates = {
                {44100, 48000, 32000}, // MPEG 1
                {22050, 24000, 16000}, // MPEG 2
                {11025, 12000, 8000}   // MPEG 2.5
        };

        return switch (version) {
            case 3 -> sampleRates[0][index];
            case 2 -> sampleRates[1][index];
            case 0 -> sampleRates[2][index];
            default -> -1;
        };
    }

    private int getBitrate(int version, int index) {

        // kbps
        int[] mpeg1 = {
                0, 32, 40, 48, 56, 64,
                80, 96, 112, 128, 160,
                192, 224, 256, 320
        };

        int[] mpeg2 = {
                0, 8, 16, 24, 32, 40,
                48, 56, 64, 80, 96,
                112, 128, 144, 160
        };

        if (version == 3) {
            return mpeg1[index];
        }

        return mpeg2[index];
    }

    private record Mp3Frame(
            int frameLength,
            int durationMs
    ) {
    }
}
