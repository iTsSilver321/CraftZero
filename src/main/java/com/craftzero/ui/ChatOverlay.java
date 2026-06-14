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
 * In-game chat HUD with modern-style command suggestions.
 */
public final class ChatOverlay {
    private static final int MAX_INPUT_LENGTH = 256;
    private static final int MAX_HISTORY = 100;
    private static final int MAX_VISIBLE_MESSAGES = 8;
    private static final int MAX_VISIBLE_SUGGESTIONS = 6;
    private static final float CLOSED_MESSAGE_TTL = 10.0f;

    private final List<ChatLine> messages = new ArrayList<>();
    private final List<String> sentHistory = new ArrayList<>();
    private final StringBuilder input = new StringBuilder();
    private List<String> suggestions = List.of();
    private String suggestionBase = "";
    private int suggestionIndex = -1;
    private int historyIndex = -1;
    private int cursor;
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
        historyIndex = sentHistory.size();
        resetSuggestions();
    }

    public void close() {
        open = false;
        input.setLength(0);
        cursor = 0;
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

        int inputY = height - 28;
        int messageY = open ? inputY - 12 : height - 82;
        int visible = 0;
        for (int i = messages.size() - 1; i >= 0 && visible < MAX_VISIBLE_MESSAGES; i--) {
            ChatLine line = messages.get(i);
            if (!open && line.age > CLOSED_MESSAGE_TTL) {
                continue;
            }
            int y = messageY - visible * 10;
            renderer.drawRect(2, y - 1, Math.min(width - 4, 320), 10, 0.0f, 0.0f, 0.0f, open ? 0.55f : 0.35f);
            renderer.drawText(line.message, 4, y, 1.0f, new float[] { 1f, 1f, 1f, open ? 1f : 0.82f });
            visible++;
        }

        if (!open) {
            return;
        }

        int suggestionCount = Math.min(MAX_VISIBLE_SUGGESTIONS, suggestions.size());
        for (int i = 0; i < suggestionCount; i++) {
            String suggestion = suggestions.get(i);
            int y = inputY - 12 - (suggestionCount - i) * 10;
            boolean selected = i == Math.max(0, suggestionIndex);
            renderer.drawRect(2, y - 1, Math.min(width - 4, 260), 10,
                    selected ? 0.22f : 0.0f, selected ? 0.22f : 0.0f, selected ? 0.22f : 0.0f, 0.72f);
            renderer.drawText(suggestion, 4, y, 1.0f,
                    selected ? new float[] { 1f, 1f, 0.45f, 1f } : new float[] { 0.75f, 0.75f, 0.75f, 1f });
        }

        renderer.drawRect(2, inputY, width - 4, 22, 0.0f, 0.0f, 0.0f, 0.70f);
        renderer.drawText("> " + renderInputWithCursor(), 4, inputY + 7, 1.0f, new float[] { 1f, 1f, 1f, 1f });
    }

    private void tick(float deltaTime) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatLine line = messages.get(i);
            line.age += Math.max(0.0f, deltaTime);
            if (!open && line.age > CLOSED_MESSAGE_TTL && messages.size() > MAX_VISIBLE_MESSAGES) {
                messages.remove(i);
            }
        }
    }

    private void handleEditingKey(int key, Function<String, List<String>> suggestionProvider) {
        if (key == GLFW_KEY_BACKSPACE) {
            if (cursor > 0) {
                input.deleteCharAt(cursor - 1);
                cursor--;
                resetSuggestions();
            }
        } else if (key == GLFW_KEY_DELETE) {
            if (cursor < input.length()) {
                input.deleteCharAt(cursor);
                resetSuggestions();
            }
        } else if (key == GLFW_KEY_LEFT) {
            cursor = Math.max(0, cursor - 1);
        } else if (key == GLFW_KEY_RIGHT) {
            cursor = Math.min(input.length(), cursor + 1);
        } else if (key == GLFW_KEY_HOME) {
            cursor = 0;
        } else if (key == GLFW_KEY_END) {
            cursor = input.length();
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

    private String renderInputWithCursor() {
        return input.substring(0, cursor) + "_" + input.substring(cursor);
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
