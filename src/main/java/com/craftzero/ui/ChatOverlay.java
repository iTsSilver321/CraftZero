package com.craftzero.ui;

import com.craftzero.ui.menu.MenuInput;
import com.craftzero.ui.menu.MenuKeys;
import com.craftzero.ui.menu.MenuRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_END;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_HOME;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;

/**
 * In-game chat HUD with Release-style translucent rows and input.
 */
public final class ChatOverlay {
    private static final int MAX_INPUT_LENGTH = 256;
    private static final int MAX_HISTORY = 100;
    private static final int MAX_CLOSED_VISIBLE_MESSAGES = 8;
    private static final int MAX_OPEN_VISIBLE_MESSAGES = 10;
    private static final int MAX_VISIBLE_SUGGESTIONS = 4;
    private static final float CLOSED_MESSAGE_TTL = 10.0f;
    private static final float CLOSED_MESSAGE_FADE_START = 7.0f;
    private static final float CURSOR_BLINK_PERIOD = 0.85f;
    private static final int CHAT_LEFT = 2;
    private static final int CHAT_WIDTH = 320;
    private static final int CHAT_LINE_HEIGHT = 9;
    private static final int CHAT_LINE_SPACING = 9;
    private static final int CHAT_TEXT_PADDING = 2;
    private static final int INPUT_HEIGHT = 12;
    private static final int INPUT_BOTTOM_MARGIN = 14;
    private static final int CLOSED_BOTTOM_MARGIN = 48;
    private static final int AVERAGE_GLYPH_WIDTH = 6;

    private final List<ChatLine> messages = new ArrayList<>();
    private final List<String> sentHistory = new ArrayList<>();
    private final StringBuilder input = new StringBuilder();
    private List<String> suggestions = List.of();
    private String suggestionBase = "";
    private int suggestionIndex = -1;
    private int historyIndex = -1;
    private int cursor;
    private float cursorBlinkTime;
    private boolean open;

    public boolean isOpen() {
        return open;
    }

    public String inputText() {
        return input.toString();
    }

    public void open(boolean commandMode) {
        open = true;
        input.setLength(0);
        if (commandMode) {
            input.append('/');
        }
        cursor = input.length();
        cursorBlinkTime = 0.0f;
        historyIndex = sentHistory.size();
        resetSuggestions();
    }

    public void close() {
        open = false;
        input.setLength(0);
        cursor = 0;
        cursorBlinkTime = 0.0f;
        resetSuggestions();
    }

    public void addMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        messages.add(new ChatLine(message, 0.0f));
        while (messages.size() > MAX_HISTORY) {
            messages.remove(0);
        }
    }

    public Optional<String> update(MenuInput menuInput, Function<String, List<String>> suggestionProvider) {
        if (!open || menuInput == null) {
            return Optional.empty();
        }

        for (int key : menuInput.pressedKeys()) {
            if (key == GLFW_KEY_ESCAPE) {
                close();
                return Optional.empty();
            }
            if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
                String submitted = input.toString().trim();
                close();
                if (!submitted.isEmpty()) {
                    sentHistory.add(submitted);
                    while (sentHistory.size() > MAX_HISTORY) {
                        sentHistory.remove(0);
                    }
                    return Optional.of(submitted);
                }
                return Optional.empty();
            }
            handleEditingKey(key, suggestionProvider);
        }

        for (char character : menuInput.typedCharacters()) {
            if (character >= 32 && character != 127 && input.length() < MAX_INPUT_LENGTH) {
                input.insert(cursor, character);
                cursor++;
                cursorBlinkTime = 0.0f;
                resetSuggestions();
            }
        }

        refreshSuggestions(suggestionProvider);
        return Optional.empty();
    }

    public void render(MenuRenderer renderer, int width, int height, float deltaTime) {
        tick(deltaTime);
        if (renderer == null) {
            return;
        }

        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        int inputY = safeHeight - INPUT_BOTTOM_MARGIN - INPUT_HEIGHT;
        int chatWidth = Math.max(0, Math.min(safeWidth - CHAT_LEFT * 2, CHAT_WIDTH));
        int inputWidth = Math.max(0, safeWidth - CHAT_LEFT * 2);
        int suggestionRows = open ? Math.min(MAX_VISIBLE_SUGGESTIONS, suggestions.size()) : 0;
        int suggestionHeight = suggestionRows == 0 ? 0 : suggestionRows * CHAT_LINE_SPACING + 2;
        int messageY = open
                ? inputY - CHAT_LINE_SPACING - suggestionHeight
                : safeHeight - CLOSED_BOTTOM_MARGIN;
        int visibleLimit = open ? MAX_OPEN_VISIBLE_MESSAGES : MAX_CLOSED_VISIBLE_MESSAGES;
        int visible = 0;
        for (int i = messages.size() - 1; i >= 0 && visible < visibleLimit; i--) {
            ChatLine line = messages.get(i);
            float alpha = messageAlpha(line);
            if (alpha <= 0.02f) {
                continue;
            }
            int y = messageY - visible * CHAT_LINE_SPACING;
            drawChatRow(renderer, CHAT_LEFT, y - 1, chatWidth, CHAT_LINE_HEIGHT,
                    open ? 0.58f : 0.34f * alpha, open);
            renderer.drawText(fitText(line.message, chatWidth - CHAT_TEXT_PADDING * 2),
                    CHAT_LEFT + CHAT_TEXT_PADDING, y, 1.0f,
                    new float[] { 1f, 1f, 1f, open ? 1f : 0.82f * alpha });
            visible++;
        }

        if (!open) {
            return;
        }

        drawSuggestionPanel(renderer, inputY, chatWidth, suggestionRows);
        drawInputPanel(renderer, inputY, inputWidth);
        renderer.drawText("> " + renderInputWithCursor(inputWidth - 14), CHAT_LEFT + CHAT_TEXT_PADDING, inputY + 2,
                1.0f, new float[] { 1f, 1f, 1f, 1f });
    }

    private void tick(float deltaTime) {
        if (open) {
            cursorBlinkTime += Math.max(0.0f, deltaTime);
            if (cursorBlinkTime > CURSOR_BLINK_PERIOD * 4.0f) {
                cursorBlinkTime = cursorBlinkTime % CURSOR_BLINK_PERIOD;
            }
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatLine line = messages.get(i);
            line.age += Math.max(0.0f, deltaTime);
            if (!open && line.age > CLOSED_MESSAGE_TTL && messages.size() > MAX_CLOSED_VISIBLE_MESSAGES) {
                messages.remove(i);
            }
        }
    }

    private float messageAlpha(ChatLine line) {
        if (open) {
            return 1.0f;
        }
        if (line.age <= CLOSED_MESSAGE_FADE_START) {
            return 1.0f;
        }
        float fadeWindow = Math.max(0.01f, CLOSED_MESSAGE_TTL - CLOSED_MESSAGE_FADE_START);
        float fade = 1.0f - (line.age - CLOSED_MESSAGE_FADE_START) / fadeWindow;
        fade = Math.max(0.0f, Math.min(1.0f, fade));
        return fade * fade;
    }

    private void handleEditingKey(int key, Function<String, List<String>> suggestionProvider) {
        if (key == GLFW_KEY_BACKSPACE) {
            if (cursor > 0) {
                input.deleteCharAt(cursor - 1);
                cursor--;
                cursorBlinkTime = 0.0f;
                resetSuggestions();
            }
        } else if (key == GLFW_KEY_DELETE) {
            if (cursor < input.length()) {
                input.deleteCharAt(cursor);
                cursorBlinkTime = 0.0f;
                resetSuggestions();
            }
        } else if (key == GLFW_KEY_LEFT) {
            cursor = Math.max(0, cursor - 1);
            cursorBlinkTime = 0.0f;
        } else if (key == GLFW_KEY_RIGHT) {
            cursor = Math.min(input.length(), cursor + 1);
            cursorBlinkTime = 0.0f;
        } else if (key == GLFW_KEY_HOME) {
            cursor = 0;
            cursorBlinkTime = 0.0f;
        } else if (key == GLFW_KEY_END) {
            cursor = input.length();
            cursorBlinkTime = 0.0f;
        } else if (key == GLFW_KEY_UP) {
            recallHistory(-1);
        } else if (key == GLFW_KEY_DOWN) {
            recallHistory(1);
        } else if (key == GLFW_KEY_TAB) {
            applySuggestion(suggestionProvider);
        }
    }

    private void recallHistory(int direction) {
        if (sentHistory.isEmpty()) {
            return;
        }
        historyIndex = Math.max(0, Math.min(sentHistory.size() - 1, historyIndex + direction));
        input.setLength(0);
        input.append(sentHistory.get(historyIndex));
        cursor = input.length();
        cursorBlinkTime = 0.0f;
        resetSuggestions();
    }

    private void applySuggestion(Function<String, List<String>> suggestionProvider) {
        refreshSuggestions(suggestionProvider);
        if (suggestions.isEmpty()) {
            return;
        }
        String base = suggestionReplacementBase();
        if (suggestionIndex >= 0 && suggestionIndex < suggestions.size()
                && base.equals(suggestions.get(suggestionIndex))) {
            suggestionIndex = (suggestionIndex + 1) % suggestions.size();
        } else if (!base.equals(suggestionBase)) {
            suggestionBase = base;
            suggestionIndex = 0;
        } else {
            suggestionIndex = (suggestionIndex + 1) % suggestions.size();
        }
        replaceCurrentToken(suggestions.get(suggestionIndex));
    }

    private void refreshSuggestions(Function<String, List<String>> suggestionProvider) {
        if (suggestionProvider == null) {
            suggestions = List.of();
            return;
        }
        suggestions = suggestionProvider.apply(input.toString());
        if (suggestions == null) {
            suggestions = List.of();
        }
        if (suggestionIndex >= suggestions.size()) {
            suggestionIndex = suggestions.isEmpty() ? -1 : 0;
        }
    }

    private void replaceCurrentToken(String suggestion) {
        int start = currentTokenStart();
        String replacement = suggestion == null ? "" : suggestion;
        input.replace(start, cursor, replacement);
        cursor = start + replacement.length();
        cursorBlinkTime = 0.0f;
    }

    private int currentTokenStart() {
        int start = cursor;
        while (start > 0 && !Character.isWhitespace(input.charAt(start - 1))) {
            start--;
        }
        return start;
    }

    private String suggestionReplacementBase() {
        int start = currentTokenStart();
        return input.substring(start, cursor);
    }

    private void resetSuggestions() {
        suggestions = List.of();
        suggestionBase = "";
        suggestionIndex = -1;
    }

    private void drawChatRow(MenuRenderer renderer, int x, int y, int rowWidth, int rowHeight,
            float alpha, boolean beveled) {
        if (rowWidth <= 0 || rowHeight <= 0 || alpha <= 0.0f) {
            return;
        }
        renderer.drawRect(x, y, rowWidth, rowHeight, 0.0f, 0.0f, 0.0f, alpha);
        if (!beveled) {
            return;
        }
        renderer.drawRect(x + 1, y, Math.max(0, rowWidth - 1), 1, 1.0f, 1.0f, 1.0f, 0.10f);
        renderer.drawRect(x, y + 1, 1, Math.max(0, rowHeight - 1), 1.0f, 1.0f, 1.0f, 0.08f);
        renderer.drawRect(x, y + rowHeight - 1, rowWidth, 1, 0.0f, 0.0f, 0.0f, 0.35f);
    }

    private void drawInputPanel(MenuRenderer renderer, int inputY, int inputWidth) {
        if (inputWidth <= 0) {
            return;
        }
        renderer.drawRect(CHAT_LEFT, inputY - 1, inputWidth, INPUT_HEIGHT + 2,
                0.0f, 0.0f, 0.0f, 0.82f);
        renderer.drawRect(CHAT_LEFT + 1, inputY, Math.max(0, inputWidth - 2), 1,
                1.0f, 1.0f, 1.0f, 0.12f);
        renderer.drawRect(CHAT_LEFT + 1, inputY + INPUT_HEIGHT, Math.max(0, inputWidth - 2), 1,
                0.0f, 0.0f, 0.0f, 0.48f);
        renderer.drawRect(CHAT_LEFT + inputWidth - 1, inputY, 1, INPUT_HEIGHT,
                0.0f, 0.0f, 0.0f, 0.44f);
    }

    private void drawSuggestionPanel(MenuRenderer renderer, int inputY, int chatWidth, int suggestionRows) {
        if (suggestionRows <= 0 || chatWidth <= 0) {
            return;
        }
        int panelTop = inputY - suggestionRows * CHAT_LINE_SPACING - 3;
        int panelHeight = suggestionRows * CHAT_LINE_SPACING + 1;
        renderer.drawRect(CHAT_LEFT, panelTop, chatWidth, panelHeight,
                0.0f, 0.0f, 0.0f, 0.72f);
        renderer.drawRect(CHAT_LEFT + 1, panelTop, Math.max(0, chatWidth - 2), 1,
                1.0f, 1.0f, 1.0f, 0.10f);

        int startIndex = suggestionStartIndex(suggestionRows);
        for (int row = 0; row < suggestionRows; row++) {
            int suggestionIndexOnList = startIndex + row;
            String suggestion = suggestions.get(suggestionIndexOnList);
            int y = panelTop + 1 + row * CHAT_LINE_SPACING;
            boolean selected = suggestionIndexOnList == suggestionIndex;
            if (selected) {
                renderer.drawRect(CHAT_LEFT + 1, y - 1, Math.max(0, chatWidth - 2), CHAT_LINE_HEIGHT,
                        0.25f, 0.25f, 0.25f, 0.62f);
            }
            float brightness = selected ? 1.0f : 0.74f;
            renderer.drawText(fitText(suggestion, chatWidth - CHAT_TEXT_PADDING * 2),
                    CHAT_LEFT + CHAT_TEXT_PADDING, y, 1.0f,
                    new float[] { brightness, brightness, brightness, 1.0f });
        }
    }

    private int suggestionStartIndex(int visibleRows) {
        if (visibleRows <= 0 || suggestions.size() <= visibleRows || suggestionIndex < 0) {
            return 0;
        }
        int centered = suggestionIndex - visibleRows / 2;
        return Math.max(0, Math.min(suggestions.size() - visibleRows, centered));
    }

    private String renderInputWithCursor(int availableWidth) {
        boolean cursorVisible = (cursorBlinkTime % CURSOR_BLINK_PERIOD) < CURSOR_BLINK_PERIOD * 0.5f;
        String cursorMark = cursorVisible ? "_" : " ";
        String rendered = input.substring(0, cursor) + cursorMark + input.substring(cursor);
        return fitTailAroundCursor(rendered, cursor, availableWidth);
    }

    private String fitTailAroundCursor(String text, int cursorIndex, int availableWidth) {
        int maxChars = Math.max(1, availableWidth / AVERAGE_GLYPH_WIDTH);
        if (text.length() <= maxChars) {
            return text;
        }
        int start = Math.max(0, cursorIndex - maxChars + 1);
        start = Math.min(start, text.length() - maxChars);
        return text.substring(start, start + maxChars);
    }

    private String fitText(String text, int availableWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int maxChars = Math.max(1, availableWidth / AVERAGE_GLYPH_WIDTH);
        if (text.length() <= maxChars) {
            return text;
        }
        if (maxChars <= 3) {
            return text.substring(0, maxChars);
        }
        return text.substring(0, maxChars - 3) + "...";
    }

    private static final class ChatLine {
        private final String message;
        private float age;

        private ChatLine(String message, float age) {
            this.message = message;
            this.age = age;
        }
    }
}
