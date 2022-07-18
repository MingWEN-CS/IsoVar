package Mutator;

import IsoVar.*;
import Util.RunCommand;
import Util.TestSuite;
import soot.*;
import soot.options.Options;

import java.io.File;
import java.util.*;

import static Similarity.Statistical.calSingleCosSimilarity;

public class MutateController {
    Configuration config;
    RunCommand cmd;


    public MutateController(Configuration config, RunCommand cmd) {
        this.config = config;
        this.cmd = cmd;
    }

    public void initialSoot() {
//        String sootClassPath = Scene.v().defaultClassPath() + ";" + instrPath;
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().setPhaseOption("jb.cp", "enabled:false");
        Options.v().set_ignore_resolving_levels(true);
//        String sootClassPath = Scene.v().defaultClassPath() + ";D:/target_classes/Time/Time_1_buggy/target/c`lasses/;target/classes/";
        String sootClassPath = Scene.v().defaultClassPath() + ";target/classes;" + config.getProjectTargetPath();
        if (MainClass.isLinux())
            sootClassPath = sootClassPath.replace(";", ":");
        Options.v().set_soot_classpath(sootClassPath);
        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_ignore_resolution_errors(true);
        // use jimple to process fields
        Options.v().set_output_format(Options.output_format_class);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_keep_line_number(true);
        Scene.v().loadNecessaryClasses();
    }

    public List<String> mutateSingleVar(VariableInfo var, int times) {
        List<String> mutants = null;
//        String mutantTemplate = "Mutants/Time/Time_1/" + var.className + "_" + var.name + "_" + var.getLine() + "_";
        String mutantTemplate = Configuration.IsoVarRoot + "/" + Configuration.mutantsRootPath + "/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + "/" + var.className + "_" +
                var.name + "_" + var.getLine() + "_";
//        System.out.println(var.className + "\t" + var.methodName + " " + var.name + " " + var.desc+" "+var.getLine());

        switch (var.type) {
            case primary:
            case primary_matrix:
                Mutator mutator = new PrimaryMutator(var, times);
                mutants = mutator.mutate(mutantTemplate);
                break;
            case object_single:
            case object_instance:
            case object_static:
                mutator = new ObjectMutator(var, times);
                mutants = mutator.mutate(mutantTemplate);
                break;

            case object_matrix:
            case multiArray:
                // do nothing
                break;
        }
        return mutants;
    }


    public void doMutate(Map<Integer, Set<String>> suitesAll,
                         Map<VariableTrimInfo, Set<VariableInfo>> varsToBeMutate,
                         Map<TestSuite, Map<Integer, Integer[]>> failingBBAccess,
                         Map<TestSuite, Map<Integer, Integer[]>> passingBBAccess) {
        initialSoot();
        for (Map.Entry<VariableTrimInfo, Set<VariableInfo>> entry : varsToBeMutate.entrySet()) {
            if (config.isSkip_mutation())
                break;
            VariableTrimInfo varTrim = entry.getKey();
            Set<String> suites = suitesAll.get(varTrim.var.methodHash);
            Set<VariableInfo> vars = entry.getValue();
            int times = config.getMax_mutation() / vars.size();
            if (times == 0)
                times += 1;
            double[] total = {0, 0};
            int actualTimes = 0;
            for (VariableInfo var : vars) {
                List<String> mutants = mutateSingleVar(var, times);
                if (mutants == null || mutants.isEmpty())
                    continue;
                actualTimes += mutants.size();
                for (String mutant : mutants) {
                    double[] diff = runTestsOnMutant(mutant, varTrim, suites, failingBBAccess, passingBBAccess);
                    total[0] += diff[0];
                    total[1] += diff[1];
                }

            }
            if (actualTimes != 0) {
                varTrim.failingDiff = total[0] / actualTimes;
                varTrim.passingDiff = total[1] / actualTimes;

                varTrim.mutate = varTrim.failingDiff - varTrim.passingDiff * config.getBeta();
            }
            varTrim.suspicious += varTrim.mutate * config.getGamma();
        }
        File mutateDir = new File(Configuration.IsoVarRoot + "/" + Configuration.mutantsRootPath + "/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + "/");
        if(mutateDir.exists()) // delete the mutants
            mutateDir.delete();

    }

    private double[] runTestsOnMutant(String mutant,
                                      VariableTrimInfo varT,
                                      Set<String> suites,
                                      Map<TestSuite, Map<Integer, Integer[]>> originFailingBBAccess,
                                      Map<TestSuite, Map<Integer, Integer[]>> originPassingBBAccess) {
        double[] diff = {0, 0};
//        double failingDiff;
//        double passingDiff;
        Map<TestSuite, Map<Integer, Integer[]>> newFailingBBAccess, newPassingBBAccess;
        newFailingBBAccess = new HashMap<>();
        newPassingBBAccess = new HashMap<>();
        String c = cmd.runCommands_tomem(newFailingBBAccess, newPassingBBAccess, mutant, suites);
        if (newFailingBBAccess.isEmpty())
            return diff;

        double totalFailing = 0;
        int cnt = 0;
        for (Map.Entry<TestSuite, Map<Integer, Integer[]>> entry : newFailingBBAccess.entrySet()) {
            TestSuite suite = entry.getKey();
            Map<Integer, Integer[]> origin = originFailingBBAccess.get(suite);
            Map<Integer, Integer[]> newAcc = entry.getValue();
            if (newAcc.containsKey(varT.var.methodHash)) {
//                totalFailing+= calSingleCosSimilarity(origin.get(var.methodHash),newAcc.get(var.methodHash));
                totalFailing += calAverageSimilarity(origin, newAcc);
                cnt++;
            }
        }
        if (cnt != 0)
            diff[0] = 1 - totalFailing / cnt;
        else diff[0] = 0;

        cnt = 0;
        double totalPassing = 0;
        for (Map.Entry<TestSuite, Map<Integer, Integer[]>> entry : newPassingBBAccess.entrySet()) {
            TestSuite suite = entry.getKey();
            Map<Integer, Integer[]> origin = originPassingBBAccess.get(suite);
            Map<Integer, Integer[]> newAcc = entry.getValue();
            if (newAcc.containsKey(varT.var.methodHash)) {
//                totalFailing+= calSingleCosSimilarity(origin.get(var.methodHash),newAcc.get(var.methodHash));
                totalPassing += calAverageSimilarity(origin, newAcc);
                cnt++;
            }
        }
        if (cnt != 0)
            diff[1] = 1 - totalPassing / cnt;
        else diff[1] = 0;

        return diff;
//        return failingDiff - passingDiff * 0.8;
    }

    double calAverageSimilarity(Map<Integer, Integer[]> origin, Map<Integer, Integer[]> newAcc) {
        double numerator = 0.00;
        double modulo = 0.00;
        List<Integer> visited = new ArrayList<>();
        for (Map.Entry<Integer, Integer[]> entry : newAcc.entrySet()) {
            int hash = entry.getKey();
            Integer[] in1 = entry.getValue();
            Integer[] in2 = origin.get(hash);
            if (in2 != null) {
                numerator += calSingleCosSimilarity(in1, in2) * in2.length;
                modulo += in2.length;
                visited.add(hash);
            } else
                modulo += in1.length;
        }
        for (Map.Entry<Integer, Integer[]> entry : origin.entrySet()) {
            if (!visited.contains(entry.getKey()))
                modulo += entry.getValue().length;
        }
        if (modulo == 0)
            return 0;
        return numerator / modulo;
    }


}
