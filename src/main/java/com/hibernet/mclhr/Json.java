package com.hibernet.mclhr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 极简 JSON 解析器（mclhr 清单只需要对象/数组/字符串/数字/布尔）。 */
final class Json {
    private final String text;
    private int index;

    private Json(String text) {
        this.text = text;
    }

    static Object parse(String text) throws MclhrException {
        Json parser = new Json(text);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.index < parser.text.length()) {
            throw new MclhrException("JSON 末尾存在多余内容 @" + parser.index);
        }
        return value;
    }

    private Object parseValue() throws MclhrException {
        skipWhitespace();
        if (index >= text.length()) {
            throw new MclhrException("JSON 意外结束");
        }
        char c = text.charAt(index);
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObject() throws MclhrException {
        expect('{');
        Map<String, Object> object = new LinkedHashMap<>();
        skipWhitespace();
        if (peek('}')) {
            index++;
            return object;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            object.put(key, parseValue());
            skipWhitespace();
            if (peek(',')) {
                index++;
                continue;
            }
            expect('}');
            return object;
        }
    }

    private List<Object> parseArray() throws MclhrException {
        expect('[');
        List<Object> array = new ArrayList<>();
        skipWhitespace();
        if (peek(']')) {
            index++;
            return array;
        }
        while (true) {
            array.add(parseValue());
            skipWhitespace();
            if (peek(',')) {
                index++;
                continue;
            }
            expect(']');
            return array;
        }
    }

    private String parseString() throws MclhrException {
        expect('"');
        StringBuilder builder = new StringBuilder();
        while (index < text.length()) {
            char c = text.charAt(index++);
            if (c == '"') {
                return builder.toString();
            }
            if (c == '\\') {
                if (index >= text.length()) {
                    throw new MclhrException("字符串转义不完整");
                }
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> builder.append(escaped);
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> {
                        if (index + 4 > text.length()) {
                            throw new MclhrException("unicode 转义不完整");
                        }
                        builder.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                        index += 4;
                    }
                    default -> throw new MclhrException("未知转义: \\" + escaped);
                }
            } else {
                builder.append(c);
            }
        }
        throw new MclhrException("字符串未闭合");
    }

    private Object parseNumber() throws MclhrException {
        int start = index;
        while (index < text.length() && "-+.eE0123456789".indexOf(text.charAt(index)) >= 0) {
            index++;
        }
        if (start == index) {
            throw new MclhrException("无法解析 JSON 值 @" + start);
        }
        try {
            return Double.parseDouble(text.substring(start, index));
        } catch (NumberFormatException e) {
            throw new MclhrException("非法数字: " + text.substring(start, index), e);
        }
    }

    private Object literal(String literal, Object value) throws MclhrException {
        if (!text.startsWith(literal, index)) {
            throw new MclhrException("非法字面量 @" + index);
        }
        index += literal.length();
        return value;
    }

    private void skipWhitespace() {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
    }

    private boolean peek(char c) {
        return index < text.length() && text.charAt(index) == c;
    }

    private void expect(char c) throws MclhrException {
        if (!peek(c)) {
            throw new MclhrException("期望 '" + c + "' @" + index);
        }
        index++;
    }
}
