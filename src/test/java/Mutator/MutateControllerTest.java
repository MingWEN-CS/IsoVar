package Mutator;

//import junit.framework.TestCase;

import IsoVar.Configuration;
import IsoVar.VariableInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.baf.BafASMBackend;
import soot.options.Options;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class MutateControllerTest {
    @Rule
    public org.junit.rules.Timeout timeout = new Timeout(15000);

    @Test(timeout = 15000)
    public void testShortMatrixWithoutIndex() {
        VariableInfo var = new VariableInfo("(short[])aa@40",
                679554901,
                "Util.Case",
                "void a()");
//        MutateController.initialSoot(";D:/target_classes/Time/Time_1_buggy/target/classes/;target/classes/");
//        MutateController.mutateSingleVar(var, 5);

    }

    @Test
    public void testShortMatrixWithIndex() {
        VariableInfo var = new VariableInfo("(short[])a[]@41",
                679554901,
                "Util.Case",
                "void a()");
//        MutateController.initialSoot(";D:/target_classes/Time/Time_1_buggy/target/classes/;target/classes/");
//        MutateController.mutateSingleVar(var, 5);

    }

    @Test
    public void testStringMatrixWithoutIndex() {
        VariableInfo var = new VariableInfo("(java.lang.String[])str2@61",
                679554901,
                "Util.Case",
                "void a()");
//        MutateController.initialSoot(";D:/target_classes/Time/Time_1_buggy/target/classes/;target/classes/");
//        MutateController.mutateSingleVar(var, 5);

    }

    @Test
    public void testStringMatrixWithIndex() {
        VariableInfo var = new VariableInfo("(java.lang.String[])str2[]@62",
                679554901,
                "Util.Case",
                "void a()");
//        MutateController.initialSoot(";D:/target_classes/Time/Time_1_buggy/target/classes/;target/classes/");
//        MutateController.mutateSingleVar(var, 5);

    }

    @Test
    public void testObjectThis() {
        // "this"
        VariableInfo var = new VariableInfo("(Util.Case2)this@-1",
                679554901,
                "Util.Case2",
                "void a()");
//        MutateController.initialSoot(";D:/target_classes/Time/Time_1_buggy/target/classes/;target/classes/");
//        MutateController.mutateSingleVar(var, 5);

    }

    @Test
    public void testObjectInstancePrimary() {
//        "(int)this.b"
        VariableInfo var = new VariableInfo("(Util.Case3)case3.b@11",
                679554901,
                "Util.Case2",
                "void a()");
//        MutateController.initialSoot(";D:/target_classes/Time/Time_1_buggy/target/classes/;target/classes/");
//        MutateController.mutateSingleVar(var, 5);

    }

    @Test
    public void testObjectInstanceObject() {
        Configuration config = new Configuration();
        config.setProjectName("Chart");
        config.setCurrent(13);
        // (object) this.x
        VariableInfo var = new VariableInfo("(Util.Case2;double)this.x@14",
                679554901,
                "Util.Case2",
                "void a()");
        MutateController controller = new MutateController(config, null);
        controller.initialSoot();
        controller.mutateSingleVar(var, 5);

    }

    @Test
    public void testObjectInstanceStatic() {
//        (primary static) this.x
        VariableInfo var = new VariableInfo("(Util.Case3).c@12",
                679554901,
                "Util.Case2",
                "void a()");
//        MutateController.initialSoot(";D:/target_classes/Time/Time_1_buggy/target/classes/;target/classes/");
//        MutateController.mutateSingleVar(var, 5);

    }

    @Test
    public void testForDebug() {
        Configuration config = new Configuration();
        config.setProjectName("Chart");
        config.setCurrent(13);
        VariableInfo var = new VariableInfo("(org.jfree.chart.util.Size2D;double)size.height:298",
                -1797312057,
                "org.jfree.chart.block.BorderArrangement",
                "org.jfree.chart.util.Size2D arrangeFN(org.jfree.chart.block.BlockContainer,java.awt.Graphics2D,double)");
        MutateController controller = new MutateController(config, null);
        controller.initialSoot();
        controller.mutateSingleVar(var, 10);
    }

    @Test
    public void aCase() throws IOException {
        VariableInfo var = new VariableInfo("(org.joda.time.PeriodType;int).YEAR_INDEX:737",
                -1797312057,
                "org.joda.time.Period",
                "int getYears()");
//        MutateController.initialSoot(";D:/target_classes/Time/Time_1_buggy/target/classes/;target/classes/");
        Scene.v().loadNecessaryClasses();
        SootClass clazz = Scene.v().getSootClass(var.className);
        for (SootMethod method : clazz.getMethods()) {
            method.setActiveBody(method.retrieveActiveBody());
        }
        File file = new File("Mutants/org/joda/time/Partial");
        file.mkdirs();
        int java_version = Options.v().java_version();
        String fileName = SourceLocator.v().getFileNameFor(clazz, Options.output_format_class);
        new File(fileName).getParentFile().mkdirs();
        OutputStream streamOut = new FileOutputStream(fileName);
        BafASMBackend backend = new BafASMBackend(clazz, java_version);
        backend.generateClassFile(streamOut);
        streamOut.close();
    }
}