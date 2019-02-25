package de.orb.wiiu.rpxgadgetfinder;

import lombok.Data;

@Data
public class GadgetSymbol extends Symbol {
    private byte[] hash;
    private int size;

    public GadgetSymbol() {
        super();
    }

    public GadgetSymbol(String hex, int size, String out) {
        super(out);
        hash = hexStringToByteArray(hex);
        this.size = size;
    }

    public byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
