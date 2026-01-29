package org.amalitech.util.db;


import org.amalitech.util.exception.DatabaseException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.amalitech.util.CamelToSnake.camelToSnake;

public class ReflectionUtils {

    public record FieldInfo(String column, Object value) {}

    public static List<FieldInfo> extractNonNullFields(Object bean, Set<String> excludeFields) {
        List<FieldInfo> fieldsList = new ArrayList<>();

        for (Field field : bean.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            String fieldName = field.getName();

            if (excludeFields.contains(fieldName)) continue;

            Object value;
            try {
                value = field.get(bean);
            } catch (IllegalAccessException e) {
                throw new DatabaseException("Failed to access: " + fieldName, e);
            }

            if (value == null) continue;

            String column = camelToSnake(fieldName);
            fieldsList.add(new FieldInfo(column, value));
        }

        return fieldsList;
    }
}

