package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The dock is the first row of the grid. dock_apps decides what is in it, or the saved order once the owner
 * changed the dock on the TV: a slot left free stays empty instead of taking the first grid app (#162) when
 * fill_dock is false; by default they fill.
 */
final class DockSlots {
    private DockSlots() { }

    /**
     * Empty slots to put after the {@code docked} dock apps found. None when the dock is full, when no
     * dock app is installed (the first row fills so Home still has a dock) or when nothing follows.
     */
    static int empty(int docked, int dockSize, int tiles, boolean fill) {
        return empty(docked, dockSize, tiles, fill, false);
    }

    /**
     * {@code emptied}: the owner took every app out of the dock (a saved dock size of 0), so with fill_dock off the
     * whole row stays empty and Home shows no dock until Down opens the grid.
     */
    static int empty(int docked, int dockSize, int tiles, boolean fill, boolean emptied) {
        if (fill || docked >= dockSize || tiles <= docked) return 0;
        if (docked <= 0) return emptied ? dockSize : 0;
        return dockSize - docked;
    }

    /**
     * The order a reorder session saves when fill_dock is false (#499). Reorder mode shows no empty slots, so a
     * grid app slides into a free dock slot without being put there. Only the keys in {@code dock} (in the dock
     * when the session started, or carried into it) that end inside the first {@code dockSize} places stay in the
     * dock: they come first, in their order, and every other key follows in its order. The dock size to save is
     * the number of keys in {@code dock} found there.
     */
    static List<String> dockFirst(List<String> keys, int dockSize, Set<String> dock) {
        List<String> head = new ArrayList<>(), tail = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            (i < dockSize && dock.contains(k) ? head : tail).add(k);
        }
        head.addAll(tail);
        return head;
    }

    /** How many of the first {@code dockSize} keys are in {@code dock}: the dock size dockFirst puts first. */
    static int dockCount(List<String> keys, int dockSize, Set<String> dock) {
        int n = 0;
        for (int i = 0; i < Math.min(dockSize, keys.size()); i++) if (dock.contains(keys.get(i))) n++;
        return n;
    }

    /** A position focus can land on: an empty slot sends it to the last dock app. */
    static int focusable(int position, int docked, int empty) {
        if (empty <= 0 || position < docked || position >= docked + empty) return position;
        return docked > 0 ? docked - 1 : docked + empty;   // an empty dock: the first grid tile
    }

    /**
     * How far a dock of {@code filled} apps in {@code dockSize} places moves right to sit in the middle of the row:
     * half the free places. A full or empty dock stays where it is.
     */
    static float centreShift(int filled, int dockSize, float cellWidth) {
        return filled > 0 && filled < dockSize ? (dockSize - filled) * cellWidth / 2f : 0f;
    }
}
