package de.orb.wiiu.rpxgadgetfinder;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SymbolList {
    private List<Symbol> symbols = new ArrayList<>();

    public void add(Symbol symbol) {
        symbols.add(symbol);
    }
}
