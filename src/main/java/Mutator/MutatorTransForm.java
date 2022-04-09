package Mutator;

import IsoVar.VariableInfo;
import soot.Body;
import soot.BodyTransformer;

import java.util.Map;

import static Instrument.Instrument.byteCodeSunSignature;

public class MutatorTransForm extends BodyTransformer {
    VariableInfo var;


    public MutatorTransForm(VariableInfo var) {
        this.var = var;

    }

    @Override
    protected void internalTransform(Body b, String s, Map<String, String> map) {
        System.out.println(byteCodeSunSignature(b.getMethod()));
    }
}
