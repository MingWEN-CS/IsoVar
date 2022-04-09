package Instrument;

import IsoVar.VariableInfo;

import java.util.ArrayList;
import java.util.List;

public class BBMapping {
    public int index;
    public List<VariableInfo> vars;

    public BBMapping(int index) {
        vars = new ArrayList<>();
        this.index = index;
    }

    public void addVar(VariableInfo var) {
        vars.add(var);
    }

}
