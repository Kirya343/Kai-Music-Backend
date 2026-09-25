package org.kirya343.features.audio.services.recognition;

import java.util.List;
import java.util.Map;

public record AcrCloudResponse(
        Metadata metadata,
        double cost_time,
        int result_type,
        Status status
) {

    public record Metadata(
            String timestamp_utc,
            List<Music> music
    ) {
    }

    public record Music(
            int score,
            int db_begin_time_offset_ms,
            int db_end_time_offset_ms,
            int sample_begin_time_offset_ms,
            int sample_end_time_offset_ms,
            int play_offset_ms,
            String acrid,
            Map<String, Object> external_ids,
            Map<String, Object> external_metadata,
            int result_from,
            String label,
            String title,
            Album album,
            int duration_ms,
            List<Artist> artists,
            String release_date,
            List<Genre> genres
    ) {
    }

    public record Album(
            String name
    ) {
    }

    public record Artist(
            String name
    ) {
    }

    public record Genre(
            String name
    ) {
    }

    public record Status(
            int code,
            String msg,
            String version
    ) {
    }
}