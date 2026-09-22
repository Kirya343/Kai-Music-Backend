package org.kirya343.features.audio.services.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.kirya343.features.audio.dto.AudioChunk;
import org.kirya343.features.storage.S3StorageService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@Slf4j 
@RequiredArgsConstructor
public class AudioStorageService {

    private final S3StorageService s3StorageService;

    public String createAudioDirectory() {
        return "/audio-" + UUID.randomUUID();
    }

    public String saveChunk(
        String audioDirectory,
        AudioChunk chunk
    ) throws IOException {

        String fileName = String.format(
            "chunk-%03d.m4a",
            chunk.sequence()
        );

        return upload(
            audioDirectory,
            fileName,
            chunk.data()
        );
    }

    public String saveInitializationChunk(
        String audioDirectory,
        AudioChunk chunk
    ) throws IOException {

        return upload(
            audioDirectory,
            "chunk-init.m4a",
            chunk.data()
        );
    }

    private String upload(
        String directory,
        String fileName,
        byte[] data
    ) throws IOException {

        return s3StorageService.upload(
            new ByteArrayInputStream(data),
            fileName,
            data.length,
            "audio/mp4",
            directory
        );
    }

    public AudioChunk getInitializationChunk(
        String audioDirectory
    ) throws IOException {

        byte[] bytes = s3StorageService.downloadBytes(
            audioDirectory + "/chunk-init.m4a"
        );

        return new AudioChunk(
            bytes,
            -1,
            0,
            true
        );
    }

    public AudioChunk getMediaChunk(
        String audioDirectory,
        int sequence
    ) throws IOException {

        String fileName = String.format(
            "chunk-%03d.m4a",
            sequence
        );

        byte[] bytes = s3StorageService.downloadBytes(
            audioDirectory + "/" + fileName
        );

        return new AudioChunk(
            bytes,
            sequence,
            5000,
            false
        );
    }

    public void deleteAudio(String audioDirectory) {

        List<S3Object> objects = s3StorageService.listFiles(audioDirectory);

        for (S3Object object : objects) {
            log.debug("deleting: " + object.key());
            s3StorageService.delete(object.key());
        }
    }
}
