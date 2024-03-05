package com.sereneoasis;

public enum SkinPart {

    CAPE(0x01),
    JACKET(0x02),
    LEFT_SLEEVE(0x04),
    RIGHT_SLEEVE(0x08),
    LEFT_PLANTS(0x10),
    RIGHT_PLANTS(0x20),
    HAT(0x40),
    ALL(0x7f);

    public static final int SKIN_PROTOCOL_ID = 17;

    private final byte id;

    SkinPart(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return this.id;
    }
}