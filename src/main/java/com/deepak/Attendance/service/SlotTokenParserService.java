package com.deepak.Attendance.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
public class SlotTokenParserService {

    public List<String> parseUniqueTokens(String slotString) {
        if (slotString == null || slotString.trim().isEmpty()) {
            return List.of();
        }

        String[] rawParts = slotString.split("[+,]");
        LinkedHashSet<String> unique = new LinkedHashSet<>();

        for (String rawPart : rawParts) {
            String normalized = rawPart == null ? "" : rawPart.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                unique.add(normalized);
            }
        }

        return new ArrayList<>(unique);
    }
}
