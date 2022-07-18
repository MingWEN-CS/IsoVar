package IsoVar;

import Instrument.InstrTransform;
import Util.Commit;
import Util.Hunk;
import Util.Pair;
import Util.Patch;
import soot.*;
import soot.options.Options;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static Instrument.InstrTransform.isJVMGeneratedMethod;
import static Instrument.Instrument.*;


public class PatchSummary {
    Configuration config;
    Set<VariableTrimInfo> buggyVars = new HashSet<>();
    Set<VariableTrimInfo> fixedVars = new HashSet<>();

    public PatchSummary(Configuration config) {
        this.config = config;
        Commit commit = new ParsePatchFiles(config.getPatchFile()).commit;
        resolvePostWithDiff(commit);
    }

    public static HashSet<VariableTrimInfo> readOracles(Configuration config, boolean reMapping) {
        File file = new File(Configuration.oracleRootPath + "/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + ".txt");
        HashSet<VariableTrimInfo> oracles = new HashSet<>();
        try {
            InputStreamReader inputReader = new InputStreamReader(Files.newInputStream(file.toPath()));
            BufferedReader bf = new BufferedReader(inputReader);
            String line;
            String[] lineSplit;
            while ((line = bf.readLine()) != null) {
                lineSplit = line.split("\t");
                String varName = lineSplit[2];
                int hash = myHashCode(lineSplit[0]) + myHashCode(lineSplit[1]);
                VariableInfo var1 = new VariableInfo(varName, hash);
                oracles.add(new VariableTrimInfo(var1));
                if (varName.endsWith("[]")) {
                    varName = varName.substring(0, varName.length() - 2);
                    VariableInfo var = new VariableInfo(varName, hash);
                    oracles.add(new VariableTrimInfo(var));
                }
                if (varName.contains(".") && !varName.startsWith(".")) {
                    varName = varName.substring(0, varName.indexOf("."));
                    VariableInfo var = new VariableInfo(varName, hash);
                    oracles.add(new VariableTrimInfo(var));
                }
            }
            bf.close();
            inputReader.close();

            if (!reMapping)
                return oracles;

            file = new File(Configuration.instrMapping + "/" + config.getProjectName() + "/" +
                    config.getProjectName() + "_" + config.getCurrent() + ".txt");
            Map<String, String> hashMap = new HashMap<>();
            inputReader = new InputStreamReader(Files.newInputStream(file.toPath()));
            bf = new BufferedReader(inputReader);
            while ((line = bf.readLine()) != null) {
                StringBuilder sb = new StringBuilder();
                lineSplit = line.split("\t");
                String className = lineSplit[0];
                String methodSig = lineSplit[1];
                int lastDot = className.lastIndexOf(".");
                sb.append(className, 0, lastDot);
                sb.append("$");
                sb.append(className, lastDot + 1, className.length());
                sb.append("#");

                int firstBlank = methodSig.indexOf(" ");
                sb.append(methodSig, firstBlank + 1, methodSig.length());

                int gzoHash = myHashCode(sb.toString()); // gzoltar style
                int myHash = myHashCode(className) + myHashCode(methodSig);
                hashMap.put(String.valueOf(myHash), String.valueOf(gzoHash));
            }
            bf.close();
            inputReader.close();

            HashSet<VariableTrimInfo> newOracles = new HashSet<>();
            for (VariableTrimInfo oracle : oracles) {
                String[] split = oracle.var.name.split(" ");
                oracle.var.name = hashMap.get(split[0]) + " " + split[1];
                newOracles.add(oracle);

                if (split[1].endsWith("[]")) {
                    oracle.var.name = hashMap.get(split[0]) + " " + split[1].substring(0, split[1].length() - 2);
                    newOracles.add(oracle);
                }
            }
            return newOracles;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return oracles;
    }

    protected void writeOracles() {
        File file = new File(Configuration.oracleRootPath + "/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + ".txt");
        File parent = file.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
        try {
            FileWriter fw = new FileWriter(file);
            for (VariableTrimInfo varT : buggyVars) {
                fw.write(varT.var.className + "\t" + varT.var.methodName + "\t");
                String name = varT.var.name.contains("#") ? varT.var.trueName : varT.var.name;
                fw.write(name + "\tbuggy\n");
            }
            for (VariableTrimInfo varT : fixedVars) {
                fw.write(varT.var.className + "\t" + varT.var.methodName + "\t");
                String name = varT.var.name.contains("#") ? varT.var.trueName : varT.var.name;
                fw.write(name + "\tfixed\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static VariableTrimInfo readOracles() {
//
//    }

    private void resolveHunk(List<String> clazzList, List<Integer> lines, boolean isBuggy) {
        G.reset();
        initializeSoot();
        if (lines.isEmpty())
            return;
        Set<VariableTrimInfo> vars;
        String classPath;
        if (isBuggy) {
            classPath = Scene.v().defaultClassPath() + ";target/classes/;" +
                    config.getProjectTargetPath();
            vars = buggyVars;
        } else {
            classPath = Scene.v().defaultClassPath() + ";target/classes/;" +
                    config.getFixedProjectTargetPath();
            vars = fixedVars;
        }
        if (MainClass.isLinux())
            classPath = classPath.replace(";", ":");
        Scene.v().setSootClassPath(classPath);
        for (String s : clazzList) {
            SootClass clazz = Scene.v().loadClassAndSupport(s);
            for (SootMethod method : clazz.getMethods()) {
                if (method.isAbstract())
                    continue;
                Body body = method.retrieveActiveBody();
                if (isJVMGeneratedMethod(body))
                    continue;

                int start = method.getJavaSourceStartLineNumber();
                int end = -1;
                for (Unit unit : body.getUnits()) {
                    end = Math.max(end, unit.getJavaSourceStartLineNumber());
                }
                boolean modify = false;
                for (Integer line : lines) {
                    if (start <= line && line <= end) {
                        modify = true;
                        break;
                    }
                }
                if (modify) {
//                    int hash = myHashCode(method.getDeclaringClass().getName()) +
//                            myHashCode(byteCodeSunSignature(method));
                    Set<VariableInfo>[] methodVars = InstrTransform.doTransverse(body);
                    for (Set<VariableInfo> BBs : methodVars) {
                        for (VariableInfo varInBB : BBs) {
                            if (lines.contains(varInBB.getLine())) {
                                vars.add(new VariableTrimInfo(varInBB));
                                if (varInBB.getLine() != -1 && !varInBB.name.contains(".")) {
                                    Pair<Integer, Boolean> info = getVarsInfo(varInBB, methodVars);
                                    String clazzRoot = config.getProjectTargetPath();
                                    String path = clazzRoot + varInBB.className.replace(".", "/") + ".class";
                                    String newName = recoverName(path, varInBB.methodName, varInBB.desc, info);
                                    if (newName == null) {
                                        varInBB.trueName = varInBB.name;
                                    } else
                                        varInBB.trueName = newName;
                                } else
                                    varInBB.trueName = varInBB.name;
                            }
                        }
                    }
                }
            }
            Scene.v().removeClass(clazz);
        }
    }

    public void resolvePostWithDiff(Commit commit) {
        for (Patch patch : commit.patches) {
            String file = patch.postFile;
            boolean isJava = file.endsWith(".java");
            // ignore non-java or non-kotlin file
            if (!(isJava || file.endsWith(".kt")))
                continue;
                // ignore test-related file
            else if (file.contains("src/test") ||
                    file.contains("src/main/test") ||
                    file.contains("/test/"))
                continue;

            if (patch.isAddNewFile) {
                System.out.println("***strange********");
            }
            file = file.replace('/', '.');
            int index = file.indexOf(config.getProjectPrefix());
            String className = file.substring(index, file.length() - 5);
            List<String> list = getClasses(className);
            for (Hunk hunk : patch.hunks) {
                if (!config.getProjectName().equals("Bears")) {
                    resolveHunk(list, hunk.getPostAffectedLines(), true);
                    resolveHunk(list, hunk.getPreAffectedLines(), false);
                } else {
                    resolveHunk(list, hunk.getPreAffectedLines(), true);
                    resolveHunk(list, hunk.getPostAffectedLines(), false);
                }
            }
        }
    }

    private List<String> getClasses(String clazzName) {
        List<String> list = new ArrayList<>();
        String prefix = clazzName.substring(0, clazzName.lastIndexOf(".")) + ".";
        String subName = clazzName.substring(clazzName.lastIndexOf(".") + 1);
        File file = new File(config.getProjectTargetPath() + clazzName.replace(".", "/") + ".class");
        File parent = file.getParentFile();
        for (File listFile : Objects.requireNonNull(parent.listFiles())) {
            if (listFile.isFile()) {
                String name = listFile.getName().substring(0, listFile.getName().length() - 6);
                if (name.contains(subName))
                    list.add(prefix + name);
            }
        }
        return list;
    }

    private void initializeSoot() {
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().setPhaseOption("jb.cp", "enabled:false");
        Options.v().set_ignore_resolving_levels(true);
//        String sootClassPath = Scene.v().defaultClassPath() + ";D:/target_classes/Time/Time_1_buggy/target/classes/;target/classes/";
//        String sootClassPath = Scene.v().defaultClassPath() + ";target/classes;" + config.getProjectTargetPath();
//        if (MainClass.isLinux())
//            sootClassPath = sootClassPath.replace(";", ":");
//        Options.v().set_soot_classpath(sootClassPath);
        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_ignore_resolution_errors(true);
        // use jimple to process fields
//        Options.v().set_output_format(Options.output_format_class);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_keep_line_number(true);
        Scene.v().loadNecessaryClasses();
    }
}



