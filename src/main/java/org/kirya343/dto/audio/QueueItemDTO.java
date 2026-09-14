package org.kirya343.dto.audio;

import java.util.Collection;
import java.util.List;

import org.kirya343.datasource.model.audio.QueueItem;

public record QueueItemDTO(
    Long id,
    Long audioId,
    String name,
    Long position
) {

    public static QueueItemDTO ofQueueItem(QueueItem qi) {
        return new QueueItemDTO(
            qi.getId(), 
            qi.getAudio().getId(),
            qi.getAudio().getTitle() != null ? qi.getAudio().getTitle() : qi.getAudio().getName(), 
            qi.getPosition()
        );
    }

    public static List<QueueItemDTO> ofList(Collection<QueueItem> queueItems) {
        return queueItems.stream().map(qi -> QueueItemDTO.ofQueueItem(qi)).toList();
    }
}
