package com.skinpainter.client.painting;

import com.skinpainter.SkinPainterMod;
import com.skinpainter.client.render.SkinTextureManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/** Snapshot-based undo/redo for skin painting. Max 20 steps. */
public class UndoRedoManager {

    private static final int MAX = 20;

    private final SkinTextureManager mgr;
    private final UUID               uuid;

    private final Deque<byte[]> undoStack = new ArrayDeque<>();
    private final Deque<byte[]> redoStack = new ArrayDeque<>();
    private       byte[]        pending   = null;

    public UndoRedoManager(SkinTextureManager mgr, UUID uuid) {
        this.mgr  = mgr;
        this.uuid = uuid;
    }

    /** Call before a paint stroke begins. */
    public void beginStroke()  { pending = mgr.exportBytes(uuid); }

    /** Call after a paint stroke ends — commits to undo stack. */
    public void commitStroke() {
        if (pending == null) return;
        push(undoStack, pending);
        redoStack.clear();
        pending = null;
    }

    public void cancelStroke() { pending = null; }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        byte[] cur = mgr.exportBytes(uuid);
        if (cur != null) push(redoStack, cur);
        mgr.loadBytes(uuid, undoStack.pop());
        SkinPainterMod.LOGGER.info("[SkinPainter] Undo ({} left)", undoStack.size());
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        byte[] cur = mgr.exportBytes(uuid);
        if (cur != null) push(undoStack, cur);
        mgr.loadBytes(uuid, redoStack.pop());
        SkinPainterMod.LOGGER.info("[SkinPainter] Redo ({} left)", redoStack.size());
        return true;
    }

    public boolean canUndo()  { return !undoStack.isEmpty(); }
    public boolean canRedo()  { return !redoStack.isEmpty(); }
    public int undoCount()    { return undoStack.size(); }
    public int redoCount()    { return redoStack.size(); }

    public void clear() { undoStack.clear(); redoStack.clear(); pending = null; }

    private static void push(Deque<byte[]> stack, byte[] data) {
        stack.push(data);
        while (stack.size() > MAX) ((ArrayDeque<byte[]>) stack).removeLast();
    }
}
