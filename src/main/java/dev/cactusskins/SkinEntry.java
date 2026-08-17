package dev.cactusskins;

import java.util.List;

/** Одна запись библиотеки скинов на сервере. */
public final class SkinEntry {

    public final String id;
    public String name;
    public final boolean slim;
    public final boolean hasCape;
    public final long ts;

    /** Ленивое превью: заполняется при первой отрисовке. */
    public net.minecraft.util.Identifier preview;
    public boolean previewRequested;
    public boolean previewFailed;

    public SkinEntry(String id, String name, boolean slim, boolean hasCape, long ts) {
        this.id = id;
        this.name = name;
        this.slim = slim;
        this.hasCape = hasCape;
        this.ts = ts;
    }

    /** Результат {@code GET /api/me}. */
    public static final class Library {
        public final String nick;
        public final String activeId;
        public final List<SkinEntry> skins;

        public Library(String nick, String activeId, List<SkinEntry> skins) {
            this.nick = nick;
            this.activeId = activeId;
            this.skins = skins;
        }
    }
}
