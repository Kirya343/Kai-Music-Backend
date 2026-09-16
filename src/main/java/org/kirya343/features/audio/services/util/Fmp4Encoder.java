package org.kirya343.features.audio.services.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Fmp4Encoder {
    
    public static byte[] encode(Path file) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException(
                "Audio file does not exist: " + file
            );
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
            "ffmpeg",
            "-hide_banner",
            "-loglevel", "error",
            "-i", file.toString(),
            "-vn",
            "-c:a", "aac",
            "-b:a", "192k",
            "-f", "mp4",
            "-movflags", "empty_moov+default_base_moof",
            "-frag_duration", "5000000",
            "pipe:1"
        );

        processBuilder.redirectError(
            ProcessBuilder.Redirect.INHERIT
        );

        Process process = processBuilder.start();

        byte[] data;

        try (InputStream input = process.getInputStream()) {
            data = input.readAllBytes();
        }

        try {
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException(
                    "FFmpeg exited with code " + exitCode
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();

            throw new IOException(
                "FFmpeg interrupted",
                e
            );
        }

        return data;
    }
}
