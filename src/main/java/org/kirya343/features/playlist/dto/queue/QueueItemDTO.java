package org.kirya343.features.playlist.dto.queue;

import java.util.Collection;
import java.util.List;

import org.kirya343.features.audio.dto.AudioDTO;
import org.kirya343.features.playlist.datasource.model.QueueItem;

public record QueueItemDTO(
    Long id,
    Long position,
    AudioDTO audio
) {

    public static QueueItemDTO ofQueueItem(QueueItem qi) {
        return new QueueItemDTO(
            qi.getId(),
            qi.getPosition(),
            AudioDTO.ofAudioFile(qi.getAudio())
        );
    }

    public static List<QueueItemDTO> ofList(Collection<QueueItem> queueItems) {
        return queueItems.stream().map(qi -> QueueItemDTO.ofQueueItem(qi)).toList();
    }
}
