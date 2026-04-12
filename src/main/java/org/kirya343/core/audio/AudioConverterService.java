package org.kirya343.core.audio;

import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.AudioInfo;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;
import org.kirya343.dto.audio.AudioMetadataDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AudioConverterService {

    public File convertToMp3(File inputFile) {
        try {
            MultimediaObject multimediaObject = new MultimediaObject(inputFile);
            MultimediaInfo info = multimediaObject.getInfo();

            String format = info.getFormat();
    
            File outputFile = new File(inputFile.getParent(), getName(inputFile) + ".mp3");

            if ("mp3".equalsIgnoreCase(format)) {
                Files.copy(
                        inputFile.toPath(),
                        outputFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
                return outputFile;
            }

            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(192000);
            audio.setChannels(2);
            audio.setSamplingRate(44100);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp3");
            attrs.setAudioAttributes(audio);

            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(inputFile), outputFile, attrs);

            return outputFile;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка конвертации аудио", e);
        }
    }

    private String getName(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf(".");
        return dot == -1 ? name : name.substring(0, dot);
    }

    public File multipartToFile(MultipartFile multipart) throws IOException {
        File file = File.createTempFile("upload_", getExtension(multipart));
        multipart.transferTo(file);
        return file;
    }

    private String getExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            return ".tmp";
        }
        return name.substring(name.lastIndexOf("."));
    }

    public AudioMetadataDTO getMetadata(File file) {
         try {

            // ⚙ техническая информация (JAVE / ffmpeg)
            MultimediaObject multimediaObject = new MultimediaObject(file);
            MultimediaInfo info = multimediaObject.getInfo();

            AudioInfo audio = info.getAudio();

            // 🎼 теги (ID3 / FLAC / M4A)
            String title = null;
            String artist = null;
            String album = null;
            String albumArtist = null;
            String composer = null;
            String genre = null;
            String comment = null;
            String year = null;
            Integer track = null;
            Integer disc = null;

            byte[] cover = null;
            String coverMime = null;

            try {
                AudioFile audioFile = AudioFileIO.read(file);
                Tag tag = audioFile.getTag();

                if (tag != null) {
                    title = tag.getFirst(FieldKey.TITLE);
                    artist = tag.getFirst(FieldKey.ARTIST);
                    album = tag.getFirst(FieldKey.ALBUM);
                    albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST);
                    composer = tag.getFirst(FieldKey.COMPOSER);
                    genre = tag.getFirst(FieldKey.GENRE);
                    comment = tag.getFirst(FieldKey.COMMENT);
                    year = tag.getFirst(FieldKey.YEAR);

                    track = parseIntSafe(tag.getFirst(FieldKey.TRACK));
                    disc = parseIntSafe(tag.getFirst(FieldKey.DISC_NO));

                    Artwork artwork = tag.getFirstArtwork();
                    if (artwork != null) {
                        cover = artwork.getBinaryData();
                        coverMime = artwork.getMimeType();
                    }
                }

            } catch (Exception ignored) {
                // файл может не иметь тегов — это нормально
            }

            return new AudioMetadataDTO(
                    emptyIfNull(title, file.getName()),
                    artist,
                    album,
                    albumArtist,
                    composer,
                    genre,
                    comment,
                    year,
                    track,
                    disc,

                    info.getDuration(),
                    info.getFormat(),
                    null,
                    audio != null ? audio.getBitRate() : null,
                    audio != null ? audio.getSamplingRate() : null,
                    audio != null ? audio.getChannels() : null,

                    cover,
                    coverMime,

                    file.getName(),
                    file.length(),

                    null
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract audio metadata", e);
        }
    }

    private Integer parseIntSafe(String value) {
        try {
            if (value == null || value.isBlank()) return null;
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private String emptyIfNull(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}