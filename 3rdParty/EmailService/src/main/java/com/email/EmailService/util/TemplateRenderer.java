package com.email.EmailService.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Simple template renderer using String replacement
 * Supports placeholders like {{variableName}}
 */
@Component
public class TemplateRenderer {
    
    public String render(String template, Map<String, Object> variables) {
        if (!StringUtils.hasText(template)) {
            return "";
        }
        
        String result = template;
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace(placeholder, value);
            }
        }
        
        return result;
    }
}

