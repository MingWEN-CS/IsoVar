package IsoVar;

import Instrument.InstrTransform;
import Util.Commit;
import Util.Hunk;
import Util.Patch;
import soot.*;
import soot.options.Options;

import java.io.*;
import java.util.*;

import static Instrument.InstrTransform.isJVMGeneratedMethod;
import static Instrument.Instrument.byteCodeSunSignature;
import static Instrument.Instrument.myHashCode;


public class PatchSummary {
    Configuration config;
    Set<VariableTrimInfo> buggyVars = new HashSet<>();
    Set<VariableTrimInfo> fixedVars = new HashSet<>();

    public PatchSummary(Configuration config) {
        this.config = config;
        Commit commit = new ParsePatchFiles(config.getPatchFile()).commit;
        resolvePostWithDiff(commit);
    }

    public static HashSet<String> readOracles(Configuration config, boolean reMapping) {
        File file = new File(config.oracleRootPath + "/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + ".txt");
        HashSet<String> oracles = new HashSet<>();
        try {
            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file));
            BufferedReader bf = new BufferedReader(inputReader);
            String line;
            String[] lineSplit;
            while ((line = bf.readLine()) != null) {
                lineSplit = line.split("\t");
                oracles.add(lineSplit[0] + " " + lineSplit[1]);
                String varName = lineSplit[1];
                if (varName.endsWith("[]"))
                    oracles.add(lineSplit[0] + " " + varName.substring(0, varName.length() - 2));
                if (varName.contains(".") && !varName.startsWith(".")) {
                    oracles.add(lineSplit[0] + " " + varName.substring(0, varName.indexOf(".")));
                }
            }
            bf.close();
            inputReader.close();

            if (!reMapping)
                return oracles;

            file = new File(config.instrMapping + "/" + config.getProjectName() + "/" +
                    config.getProjectName() + "_" + config.getCurrent() + ".txt");
            Map<String, String> hashMap = new HashMap<>();
            inputReader = new InputStreamReader(new FileInputStream(file));
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

            HashSet<String> newOracles = new HashSet<>();
            for (String oracle : oracles) {
                String[] split = oracle.split(" ");
                newOracles.add(hashMap.get(split[0]) + " " + split[1]);
                if (split[1].endsWith("[]"))
                    newOracles.add(hashMap.get(split[0]) + " " + split[1].substring(0, split[1].length() - 2));
            }
            return newOracles;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return oracles;
    }

    protected void writeOracles() {
        File file = new File(config.oracleRootPath + "/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + ".txt");
        File parent = file.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
        try {
            FileWriter fw = new FileWriter(file);
            for (VariableTrimInfo var : buggyVars) {
                fw.write(var.methodHash + "\t" + var.name + "\tbuggy\n");
            }
            for (VariableTrimInfo var : fixedVars) {
                fw.write(var.methodHash + "\t" + var.name + "\tfixed\n");
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
                int end = body.getUnits().getLast().getJavaSourceStartLineNumber();
                boolean modify = false;
                for (Integer line : lines) {
                    if (start <= line && line <= end) {
                        modify = true;
                        break;
                    }
                }
                if (modify) {
                    int hash = myHashCode(method.getDeclaringClass().getName()) +
                            myHashCode(byteCodeSunSignature(method));
                    Set<VariableInfo>[] methodVars = InstrTransform.doTransverse(body);
                    for (Set<VariableInfo> BBs : methodVars) {
                        for (VariableInfo varInBB : BBs) {
                            if (lines.contains(varInBB.getLine())) {
//                                varInBB.className = method.getDeclaringClass().getName();
//                                varInBB.methodName = method.getSubSignature();
//                                varInBB.methodHash = hash;
                                vars.add(new VariableTrimInfo(varInBB.name, varInBB.desc, hash, varInBB.isClinit));
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
                    file.contains("Test"))
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
        for (File listFile : parent.listFiles()) {
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



