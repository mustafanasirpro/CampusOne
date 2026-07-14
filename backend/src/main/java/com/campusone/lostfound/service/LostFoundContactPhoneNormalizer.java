package com.campusone.lostfound.service;

import com.campusone.lostfound.exception.LostFoundConflictException;
import org.springframework.stereotype.Component;

@Component
public class LostFoundContactPhoneNormalizer {

    private static final String INVALID_MESSAGE =
            "Enter a valid handover contact number.";

    public String normalize(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw invalid();
        }
        for (int index = 0; index < rawPhone.length(); index++) {
            if (Character.isISOControl(rawPhone.charAt(index))) {
                throw invalid();
            }
        }
        String trimmed = rawPhone.trim();
        StringBuilder normalized = new StringBuilder(trimmed.length());
        int plusCount = 0;
        for (int index = 0; index < trimmed.length(); index++) {
            char current = trimmed.charAt(index);
            if (current == '+') {
                plusCount++;
                normalized.append(current);
                continue;
            }
            if (Character.isDigit(current)) {
                normalized.append(current);
                continue;
            }
            if (current == ' '
                    || current == '-'
                    || current == '('
                    || current == ')') {
                continue;
            }
            throw invalid();
        }

        String value = normalized.toString();
        if (value.isBlank() || plusCount > 1) {
            throw invalid();
        }
        if (value.startsWith("+")) {
            if (value.indexOf('+', 1) >= 0) {
                throw invalid();
            }
            if (value.matches("^\\+[1-9][0-9]{7,14}$")) {
                return value;
            }
            throw invalid();
        }
        if (value.indexOf('+') >= 0) {
            throw invalid();
        }
        if (value.matches("^03[0-9]{9}$")) {
            return "+92" + value.substring(1);
        }
        throw invalid();
    }

    private LostFoundConflictException invalid() {
        return new LostFoundConflictException(INVALID_MESSAGE);
    }
}
