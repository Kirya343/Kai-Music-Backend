package org.kirya343.features.audio.services.storage;

import java.io.File;
import java.util.List;

import org.kirya343.features.audio.services.util.AudioConverter;
import org.kirya343.features.audio.services.util.Fmp4Parser;
import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.audio.datasource.AudioFileRepository;
import org.kirya343.features.user.datasource.User;
import org.kirya343.features.audio.dto.AudioChunk;
import org.kirya343.features.audio.dto.AudioMetadataDTO;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j 
@RequiredArgsConstructor
public class AudioFileManager {

    private final AudioFileRepository audioFileRepository;
    private final AudioStorageService audioStorageService;
    private final EntityManager entityManager;

    public void uploadAudio(MultipartFile uploadedFile, UserAuthData authData) {

        log.debug("Пользователь {} загружает аудио на сервер", authData.name());

        File tempInput = null;
        File converted = null;

        try {
            // 1. Multipart → File
            tempInput = AudioConverter.multipartToFile(uploadedFile);
            log.debug("Конвертировали из MultipartFile в File");

            AudioMetadataDTO metadata = AudioConverter.getMetadata(tempInput);

            log.debug("Полученные метаданные файла: {}", metadata.toString());

            // 2. Конвертация → MP3
            converted = AudioConverter.convertToMp3(tempInput);
            log.debug("Конвертировали в mp3");

            // 3. Название
            String originalName = uploadedFile.getOriginalFilename();

            log.debug("Оригинальное название файла: {}", originalName);

            if (originalName != null && originalName.contains(".")) {
                originalName = originalName.substring(0, originalName.lastIndexOf("."));
            }

            Fmp4Parser parser = new Fmp4Parser(converted.toPath());

            String audioDirectory = audioStorageService.createAudioDirectory();

            audioStorageService.saveInitializationChunk(
                audioDirectory,
                parser.getInitializationChunk()
            );

            List<AudioChunk> chunks = parser.getAudioChunks();
            for (AudioChunk chunk : chunks) {
                audioStorageService.saveChunk(
                    audioDirectory,
                    chunk
                );
            }

            String title = metadata.title().startsWith("upload") ? originalName : metadata.title();

            // 5. Сохраняем в БД
            AudioFile audio = new AudioFile(
                title,
                audioDirectory,
                "mp3",
                entityManager.getReference(User.class, authData.id()),
                metadata.title(),
                metadata.artist(),
                metadata.album(),
                null,
                metadata.durationMs() / 1000,
                chunks.size()
            );
            
            log.debug("Сохранили файл в бд");

            audioFileRepository.save(audio);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки аудио", e);
        } finally {
            // 6. Чистим временные файлы
            if (tempInput != null && tempInput.exists()) tempInput.delete();
            if (converted != null && converted.exists()) converted.delete();
        }
    }
}
