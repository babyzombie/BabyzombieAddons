package top.babyzombie.addons.module.chat;

public enum ChatChannel {
    GC("GC", "/chat g",    0xFF55FF55),
    OC("OC", "/chat o",    0xFF3333CC),
    PC("PC", "/chat p",    0xFF5555FF),
    CC("CC", "/chat coop", 0xFF55FFFF),
    AC("AC", "/chat a",    0xFFFFAA00);

    public final String label;
    public final String command;
    public final int activeColor;

    ChatChannel(String label, String command, int activeColor) {
        this.label = label;
        this.command = command;
        this.activeColor = activeColor;
    }
}
