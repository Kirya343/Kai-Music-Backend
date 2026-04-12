package org.kirya343.dto.audio;

public record AudioMetadataDTO(

    // 🎼 Основная информация
    String title,
    String artist,
    String album,
    String albumArtist,
    String composer,
    String genre,
    String comment,
    String year,
    Integer trackNumber,
    Integer discNumber,

    // ⏱ Технические параметры
    Long durationMs,
    String format,
    String codec,
    Integer bitrate,
    Integer sampleRate,
    Integer channels,

    // 🖼 Обложка
    byte[] coverImage,
    String coverMimeType,

    // 📁 Файл
    String fileName,
    Long fileSize,

    // 🧠 Сырой fallback (если нужно сохранить всё что не распарсилось)
    String rawMetadata
) {}