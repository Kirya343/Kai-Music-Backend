package org.kirya343.core.audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.kirya343.datasource.model.audio.AudioFile;
import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.repository.audio.AudioFileRepository;
import org.kirya343.dto.audio.AudioMetadataDTO;
import org.kirya343.dto.auth.UserAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioFileManager {

    private final AudioFileRepository audioFileRepository;
    private final EntityManager entityManager;
    private final AudioConverterService audioConverterService;
    private static final Logger logger = LoggerFactory.getLogger(AudioFileManager.class);

    public ResponseEntity<InputStreamResource> getAudio(
        Long queueItemId, 
        String rangeHeader, 
        UserAuthData authData
    ) throws IOException {

        AudioFile audio = audioFileRepository.findAudioInUserRoom(authData.id(), queueItemId)
                .orElseThrow(() -> new AccessDeniedException("Нет доступа к этому треку"));

        File audioFile = new File(audio.getPath());
        long fileLength = audioFile.length();

        String contentType = Files.probeContentType(audioFile.toPath());
        logger.debug("contentType: {}", contentType);
        if (contentType == null) {
            contentType = "application/octet-stream"; // fallback
        }

        if (rangeHeader == null) {
            // Отдаём весь файл целиком
            FileInputStream fis = new FileInputStream(audioFile);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + audioFile.getName())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentLength(fileLength)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new InputStreamResource(fis));
        }

        // Парсим Range: bytes=START-
        String[] ranges = rangeHeader.replace("bytes=", "").split("-");
        long start = Long.parseLong(ranges[0]);
        long end = ranges.length > 1 && !ranges[1].isEmpty() ? Long.parseLong(ranges[1]) : fileLength - 1;
        long contentLength = end - start + 1;

        FileInputStream fis = new FileInputStream(audioFile);
        fis.skip(start);
        InputStreamResource resource = new InputStreamResource(new LimitedInputStream(fis, contentLength));

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + audioFile.getName())
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                .body(resource);
    }

    public void uploadAudio(MultipartFile uploadedFile, UserAuthData authData) {

        logger.info("Пользователь {} загружает аудио на сервер", authData.name());

        File tempInput = null;
        File converted = null;

        try {
            // 1. Multipart → File
            tempInput = audioConverterService.multipartToFile(uploadedFile);
            logger.info("Конвертировали из MultipartFile в File");

            AudioMetadataDTO metadata = audioConverterService.getMetadata(tempInput);

            logger.info("Полученные метаданные файла: {}", metadata.toString());

            // 2. Конвертация → MP3
            converted = audioConverterService.convertToMp3(tempInput);
            logger.info("Конвертировали в mp3");

            // 3. Название
            String originalName = uploadedFile.getOriginalFilename();
            String title = originalName;

            logger.info("Оригинальное название файла: {}", originalName);

            if (originalName != null && originalName.contains(".")) {
                title = originalName.substring(0, originalName.lastIndexOf("."));
            }

            logger.info("Итоговое название файла: {}", title);

            // ❗ всегда mp3 после конвертации
            String name = UUID.randomUUID().toString() + ".mp3";

            Path path = Paths.get("music/" + name);
            Files.createDirectories(path.getParent());
            logger.info("Проверили правильность пути записи");

            // 4. Копируем файл
            Files.copy(converted.toPath(), path, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Записали файл в хранилище");

            // 5. Сохраняем в БД
            AudioFile audio = new AudioFile(
                title,
                path.toString(),
                "mp3",
                entityManager.getReference(User.class, authData.id()),
                metadata.title(),
                metadata.artist(),
                metadata.album(),
                null,
                metadata.durationMs() / 1000
            );
            
            logger.info("Сохранили файл в бд");

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
