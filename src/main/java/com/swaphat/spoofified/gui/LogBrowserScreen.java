package com.swaphat.spoofified.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LogBrowserScreen extends Screen {
    private EditBox searchBox;
    private LogList list;
    private List<String> allLines = new ArrayList<>();
    private final Path logPath;

    public LogBrowserScreen() {
        super(Component.literal("Live Log Browser"));
        this.logPath = Minecraft.getInstance().gameDirectory.toPath().resolve("logs/latest.log");
    }

    @Override
    protected void init() {
        try {
            allLines = Files.readAllLines(logPath);
        } catch (Exception e) {
            allLines.add("Failed to load logs: " + e.getMessage());
        }

        // 1. Make the search box wider and center it clearly
        searchBox = new EditBox(this.font, this.width / 2 - 150, 20, 300, 20, Component.literal("Search"));
        searchBox.setResponder(this::updateSearch);
        this.addRenderableWidget(searchBox);

        this.setInitialFocus(searchBox);

        list = new LogList(this.minecraft, this.width, this.height - 55, 55, 25);
        this.addRenderableWidget(list);

        updateSearch("");
    }

    private void updateSearch(String query) {
        list.clearEntries();
        String lowerQuery = query.toLowerCase();

        for (int i = 0; i < allLines.size(); i++) {
            String line = allLines.get(i);
            if (query.isEmpty() || line.toLowerCase().contains(lowerQuery)) {
                list.addEntry(new LogEntry(i, line));
            }
        }
    }

    private void deleteLine(int originalIndex) {
        if (originalIndex >= 0 && originalIndex < allLines.size()) {
            allLines.remove(originalIndex);
            try {
                Files.write(logPath, allLines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            updateSearch(searchBox.getValue());
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        graphics.text(this.font, "Search logs:", this.width / 2 - 150, 8, 0xFFFFFFFF, false);

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    class LogList extends ObjectSelectionList<LogEntry> {
        public LogList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return this.width - 24;
        }

        @Override
        public int getRowLeft() {
            return 12;
        }

        @Override
        protected int scrollBarX() {
            return this.width - 8;
        }

        public void clearEntries() {
            super.clearEntries();
        }

        public int addEntry(@NonNull LogEntry entry) {
            super.addEntry(entry);
            return 0;
        }
    }

    class LogEntry extends ObjectSelectionList.Entry<LogEntry> {
        private final int originalIndex;
        private final String line;
        private final Button deleteBtn;

        public LogEntry(int originalIndex, String line) {
            this.originalIndex = originalIndex;
            this.line = line;
            this.deleteBtn = Button.builder(Component.literal("§c✕"), b -> {
                deleteLine(this.originalIndex);
            }).bounds(0, 0, 20, 20).build();
        }

        @Override
        public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            this.deleteBtn.setX(this.getX());
            this.deleteBtn.setY(this.getY());
            this.deleteBtn.extractRenderState(graphics, mouseX, mouseY, a);

            String trimmed = font.plainSubstrByWidth(line, this.getWidth() - 30);

            graphics.text(font, trimmed, this.getX() + 25, this.getY() + 6, 0xFFCCCCCC, false);
        }

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
            if (this.deleteBtn.mouseClicked(event, doubleClick)) {
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public void visitWidgets(Consumer<net.minecraft.client.gui.components.AbstractWidget> widgetVisitor) {
            widgetVisitor.accept(this.deleteBtn);
        }

        @Override
        public @NonNull Component getNarration() {
            return Component.literal(this.line);
        }
    }
}