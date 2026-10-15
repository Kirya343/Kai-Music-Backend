package org.kirya343.features.playback.diagnostics;

import org.kirya343.infrastructure.diagnostics.commands.DiagnosticCommand;
import org.kirya343.infrastructure.diagnostics.models.DiagnosticResult;
import org.springframework.stereotype.Component;

@Component
public class ListRoomStreamersCommand implements DiagnosticCommand {

    @Override
    public String getName() {
        return "playback.rooms";
    }

    @Override
    public String getDescription() {
        return "Lists active room streamers";
    }

    @Override
    public DiagnosticResult execute(String[] args) {
        // Получение состояния комнатных стримеров.
        // Формирование результата диагностики.
        return null;
    }
}