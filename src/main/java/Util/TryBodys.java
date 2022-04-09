package Util;

import IsoVar.MainClass;
import IsoVar.VariableInfo;
import soot.*;
import soot.baf.BafASMBackend;
import soot.options.Options;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class TryBodys {
    public static void main(String[] args) {
        initialSoot();
        Scene.v().loadClassAndSupport("Util.Case2");
        Scene.v().loadNecessaryClasses();
        SootClass clazz = Scene.v().getSootClass("Util.Case2");
        for (SootMethod method : clazz.getMethods()) {
            if (method.getName().equals("a")) {
                Body b = method.retrieveActiveBody();
                int a = 0;
            }
        }
    }

    private static void initialSoot() {
        Options.v().setPhaseOption("jb", "use-original-names:true");
//        String sootClassPath = Scene.v().defaultClassPath() + ";" + instrPath;
        String sootClassPath = Scene.v().defaultClassPath() + ";target/classes/";
        if (MainClass.isLinux())
            sootClassPath = sootClassPath.replace(";", ":");
        Options.v().set_soot_classpath(sootClassPath);
//        Options.v().set_whole_program(true);
        Options.v().set_src_prec(Options.src_prec_only_class);
        // use jimple to process fields
//        Options.v().set_output_format(soot.options.Options.output_format_n);

        Options.v().set_output_format(soot.options.Options.output_format_J);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_keep_line_number(true);
    }
}
