package org.kirya343.core.audio;

import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AudioConverterService {

    public File convertToMp3(File inputFile) {
        try {
            File outputFile = new File(inputFile.getParent(), getName(inputFile) + ".mp3");

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
}