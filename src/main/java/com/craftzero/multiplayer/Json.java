package com.craftzero.multiplayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {
    }

    static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw parser.error("Unexpected trailing characters");
        }
        return value;
    }

    static String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value);
        return builder.toString();
    }

    private static void writeValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String string) {
            writeString(builder, string);
        } else if (value instanceof Number number) {
            writeNumber(builder, number);
        } else if (value instanceof Boolean bool) {
            builder.append(bool.booleanValue());
        } else if (value instanceof Map<?, ?> map) {
            writeObject(builder, map);
        } else if (value instanceof Iterable<?> iterable) {
            writeArray(builder, iterable);
        } else {
            writeString(builder, String.valueOf(value));
        }
    }

    private static void writeObject(StringBuilder builder, Map<?, ?> map) {
        builder.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            writeString(builder, String.valueOf(entry.getKey()));
            builder.append(':');
            writeValue(builder, entry.getValue());
        }
        builder.append('}');
    }

    private static void writeArray(StringBuilder builder, Iterable<?> iterable) {
        builder.append('[');
        boolean first = true;
        for (Object value : iterable) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            writeValue(builder, value);
        }
        builder.append(']');
    }

    private static void writeNumber(StringBuilder builder, Number number) {
        if (number instanceof Double doubleValue && !Double.isFinite(doubleValue)) {
            builder.append("null");
            return;
        }
        if (number instanceof Float floatValue && !Float.isFinite(floatValue)) {
            builder.append("null");
            return;
        }
        builder.append(number);
    }

    private static void writeString(StringBuilder builder, String value) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        builder.append('"');
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text == null ? "" : text;
        }

        private Object parseValue() {
            skipWhitespace();
            if (isAtEnd()) {
                throw error("Expected JSON value");
            }
            char ch = peek();
            return switch (ch) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (ch == '-' || Character.isDigit(ch)) {
                        yield parseNumber();
                    }
                    throw error("Unexpected character '" + ch + "'");
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            LinkedHashMap<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (tryConsume('}')) {
                return object;
            }
            do {
                skipWhitespace();
                if (peek() != '"') {
                    throw error("Expected object key");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
            } while (tryConsume(','));
            expect('}');
            return object;
        }

        private List<Object> parseArray() {
            expect('[');
            ArrayList<Object> array = new ArrayList<>();
            skipWhitespace();
            if (tryConsume(']')) {
                return array;
            }
            do {
                array.add(parseValue());
                skipWhitespace();
            } while (tryConsume(','));
            expect(']');
            return array;
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (!isAtEnd()) {
                char ch = next();
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch != '\\') {
                    builder.append(ch);
                    continue;
                }
                if (isAtEnd()) {
                    throw error("Unterminated escape sequence");
                }
                char escape = next();
                switch (escape) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> builder.append(parseUnicodeEscape());
                    default -> throw error("Unsupported escape sequence '\\" + escape + "'");
                }
            }
            throw error("Unterminated string");
        }

        private char parseUnicodeEscape() {
            if (index + 4 > text.length()) {
                throw error("Incomplete unicode escape");
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                char ch = next();
                int digit = Character.digit(ch, 16);
                if (digit < 0) {
                    throw error("Invalid unicode escape");
                }
                value = (value << 4) + digit;
            }
            return (char) value;
        }

        private Object parseLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw error("Expected '" + literal + "'");
            }
            index += literal.length();
            return value;
        }

        private Number parseNumber() {
            int start = index;
            tryConsume('-');
            consumeDigits();
            boolean decimal = false;
            if (tryConsume('.')) {
                decimal = true;
                consumeDigits();
            }
            if (tryConsume('e') || tryConsume('E')) {
                decimal = true;
                if (!tryConsume('+')) {
                    tryConsume('-');
                }
                consumeDigits();
            }

            String token = text.substring(start, index);
            if (decimal) {
                return Double.parseDouble(token);
            }
            try {
                return Long.parseLong(token);
            } catch (NumberFormatException ignored) {
                return Double.parseDouble(token);
            }
        }

        private void consumeDigits() {
            int start = index;
            while (!isAtEnd() && Character.isDigit(peek())) {
                index++;
            }
            if (start == index) {
                throw error("Expected digit");
            }
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(peek())) {
                index++;
            }
        }

        private boolean tryConsume(char expected) {
            if (!isAtEnd() && peek() == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (isAtEnd() || next() != expected) {
                throw error("Expected '" + expected + "'");
            }
        }

        private char peek() {
            return text.charAt(index);
        }

        private char next() {
            return text.charAt(index++);
        }

        private boolean isAtEnd() {
            return index >= text.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + index);
        }
    }
}
