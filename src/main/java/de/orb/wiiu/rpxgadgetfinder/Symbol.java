package de.orb.wiiu.rpxgadgetfinder;

import lombok.Data;

@Data
public class Symbol {
    private String out;

    public Symbol() {
    }

    public Symbol(String out) {
        this.out = out;
    }

}
