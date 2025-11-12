package com.softuni.gms.app.web.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.UUID;

@UtilityClass
public class RequestUtils {

    public static UUID getPathVariableAsUuid(HttpServletRequest request, String variableName) {

        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map<?, ?> pathVariables)) {
            return null;
        }

        Object value = pathVariables.get(variableName);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(stringValue);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

