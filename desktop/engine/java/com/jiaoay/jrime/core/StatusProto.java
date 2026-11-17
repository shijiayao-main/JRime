package com.jiaoay.jrime.core;

public class StatusProto {
    public final String schemaId;
    public final String schemaName;
    public final boolean isAsciiMode;
    public final boolean isComposing;
    public final boolean isAsciiPunch;
    public final boolean isSimplified;
    public final boolean isFullShape;
    public final boolean isAsciiShape;
    public final boolean isAsciiComma;

    public StatusProto(String schemaId, String schemaName, boolean isAsciiMode,
                       boolean isComposing, boolean isAsciiPunch, boolean isSimplified,
                       boolean isFullShape, boolean isAsciiShape, boolean isAsciiComma) {
        this.schemaId = schemaId;
        this.schemaName = schemaName;
        this.isAsciiMode = isAsciiMode;
        this.isComposing = isComposing;
        this.isAsciiPunch = isAsciiPunch;
        this.isSimplified = isSimplified;
        this.isFullShape = isFullShape;
        this.isAsciiShape = isAsciiShape;
        this.isAsciiComma = isAsciiComma;
    }
}
