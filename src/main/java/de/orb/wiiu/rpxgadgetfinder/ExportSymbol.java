package de.orb.wiiu.rpxgadgetfinder;

import lombok.Data;

@Data
public class ExportSymbol extends Symbol {
    private String name;

    public ExportSymbol() {
        super();
    }

    public ExportSymbol(String name, String out) {
        super(out);
        this.name = name;
    }

}
