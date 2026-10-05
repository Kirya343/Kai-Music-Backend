package org.kirya343.features.audio.services.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.audio.dto.AudioChunk;
import org.kirya343.features.audio.services.storage.AudioStorageService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component 
@Slf4j 
@RequiredArgsConstructor 
public class M4aBuilder {

    private final AudioStorageService audioStorageService;

    public byte[] buildM4a(
            AudioChunk initialization,
            List<AudioChunk> mediaChunks
    ) throws IOException, InterruptedException {

        ByteArrayOutputStream input = new ByteArrayOutputStream();

        input.writeBytes(initialization.data());

        mediaChunks.stream()
                .filter(chunk -> !chunk.initialization())
                .sorted(Comparator.comparingLong(AudioChunk::sequence))
                .limit(6)
                .forEach(chunk -> input.writeBytes(chunk.data()));

        Path inputFile = Files.createTempFile(
                "acrcloud-input-",
                ".m4a"
        );

        Path outputFile = Files.createTempFile(
                "acrcloud-output-",
                ".m4a"
        );

        try {
            Files.write(
                    inputFile,
                    input.toByteArray()
            );

            log.info(
                    "FFmpeg input created: path={}, bytes={}",
                    inputFile,
                    Files.size(inputFile)
            );

            Process process = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-hide_banner",
                    "-loglevel", "error",

                    "-i", inputFile.toString(),

                    "-map", "0:a:0",
                    "-vn",
                    "-c:a", "copy",
                    "-movflags", "+faststart",

                    outputFile.toString()
            )
                    .redirectErrorStream(true)
                    .start();

            process.getOutputStream().close();

            String ffmpegOutput;

            try (InputStream stream = process.getInputStream()) {
                ffmpegOutput = new String(
                        stream.readAllBytes(),
                        StandardCharsets.UTF_8
                );
            }

            int exitCode = process.waitFor();

            log.info(
                    "FFmpeg finished: exitCode={}, output={}",
                    exitCode,
                    ffmpegOutput
            );

            if (exitCode != 0) {
                throw new IOException(
                        "FFmpeg failed: " + ffmpegOutput
                );
            }

            byte[] result = Files.readAllBytes(outputFile);

            log.info(
                    "FFmpeg output created: path={}, bytes={}",
                    outputFile,
                    result.length
            );

            return result;

        } finally {
            Files.deleteIfExists(inputFile);
            Files.deleteIfExists(outputFile);
        }
    }
    
    public byte[] buildRecognizeM4a(AudioFile audioFile) throws IOException {

        long storageStartedAt = System.currentTimeMillis();

        AudioChunk init = audioStorageService
                .getInitializationChunk(audioFile.getPath());

        log.info(
                "Initialization chunk loaded: audioId={}, sequence={}, bytes={}, duration={}, initialization={}",
                audioFile.getId(),
                init.sequence(),
                init.data().length,
                init.initialization()
        );

        List<AudioChunk> media = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            AudioChunk chunk = audioStorageService
                    .getMediaChunk(audioFile.getPath(), i);

            media.add(chunk);

            log.info(
                    "Media chunk loaded: audioId={}, index={}, sequence={}, bytes={}, duration={}, initialization={}",
                    audioFile.getId(),
                    i,
                    chunk.sequence(),
                    chunk.data().length,
                    chunk.initialization()
            );
        }

        log.info(
                "All chunks loaded: audioId={}, mediaChunks={}, storageTime={}ms",
                audioFile.getId(),
                media.size(),
                System.currentTimeMillis() - storageStartedAt
        );

        byte[] m4a;

        long ffmpegStartedAt = System.currentTimeMillis();

        try {
            log.info(
                    "Building M4A with FFmpeg: audioId={}, chunks={}",
                    audioFile.getId(),
                    media.size()
            );

            m4a = buildM4a(init, media);

            log.info(
                    "M4A built successfully: audioId={}, bytes={}, ffmpegTime={}ms",
                    audioFile.getId(),
                    m4a.length,
                    System.currentTimeMillis() - ffmpegStartedAt
            );

        } catch (IOException | InterruptedException e) {
            log.error(
                    "Failed to build M4A: audioId={}, time={}ms",
                    audioFile.getId(),
                    System.currentTimeMillis() - ffmpegStartedAt,
                    e
            );

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new IOException(
                    "Failed to build M4A for audio " + audioFile.getId(),
                    e
            );
        }

        return m4a;
    }
}
