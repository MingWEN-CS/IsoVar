package Instrument;

import IsoVar.*;
import Util.Pair;
import Util.RunCommand;
import Util.TestSuite;
import jas.Var;
import org.apache.commons.io.FileUtils;
import org.javatuples.Quartet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.baf.BafASMBackend;
import soot.options.Options;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.ASTORE;


public class Instrument {
    public final Logger logger = LoggerFactory.getLogger(getClass());
    Configuration config;

    public Instrument(Configuration config) {
        this.config = config;
    }

    //需要注意的是当删除某一目录时，必须保证该目录下没有其他文件才能正确删除，否则将删除失败。
    public static void deleteFolder(File folder) throws Exception {
        if (!folder.exists()) {
            throw new Exception("dir do not exist");
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();

    }

    public static Map<TestSuite, Map<Integer, Integer[]>> readBBAccess(String reportDir, List<TestSuite> suites) {
        Map<TestSuite, Map<Integer, Integer[]>> bbAccess = new HashMap<>();
        for (TestSuite suite : suites) {
            Map<Integer, Integer[]> bb = readSingleTest(reportDir, suite);
            if (bb != null && bb.size() > 3)
                bbAccess.put(suite, bb);
        }
        return bbAccess;
    }

    public static Map<Integer, BBMapping[]> readInstrMapping(Configuration config) {
        Map<Integer, BBMapping[]> instrMapping = new HashMap<>();
        File file = new File(Configuration.instrMapping + "/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + ".txt");
        InputStreamReader inputReader;
        try {
            inputReader = new InputStreamReader(Files.newInputStream(file.toPath()));
            BufferedReader bf = new BufferedReader(inputReader);
            String line;
            String[] lineSplit;
            while ((line = bf.readLine()) != null) {
                lineSplit = line.split("\t");
                int methodHash = Integer.parseInt(lineSplit[2]);
                BBMapping[] bbMappings = new BBMapping[lineSplit.length - 3];
                for (int i = 3; i < lineSplit.length; i++) {
                    String[] bb = lineSplit[i].split(" ");
                    BBMapping mapping = new BBMapping(Integer.parseInt(bb[0]));
                    for (int j = 1; j < bb.length; j++) {
                        String nameDesc = bb[j];
                        if (nameDesc.contains("Exception"))
                            continue;
                        int rightBrackets = nameDesc.indexOf(")");
                        int second = nameDesc.indexOf("@");
                        if (second == -1)
                            second = nameDesc.indexOf(":");
                        String name = nameDesc.substring(rightBrackets + 1, second);
                        if (name.contains("$"))
                            continue;
                        mapping.addVar(new VariableInfo(bb[j], methodHash, lineSplit[0], lineSplit[1]));
                    }
                    bbMappings[i - 3] = mapping;
                }
                instrMapping.put(methodHash, bbMappings);

            }
            bf.close();
            inputReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return instrMapping;
    }

    public static Map<Integer, Map<VariableTrimInfo, Set<Integer>>> readInstrMapping_Bears(Configuration config) {
        Map<Integer, Map<VariableTrimInfo, Set<Integer>>> instrMapping = new HashMap<>();
        File file = new File(Configuration.instrMapping + "/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + ".txt");
        InputStreamReader inputReader;
        try {
            inputReader = new InputStreamReader(Files.newInputStream(file.toPath()));
            BufferedReader bf = new BufferedReader(inputReader);
            String line;
            String[] lineSplit;
            while ((line = bf.readLine()) != null) {
                Map<VariableTrimInfo, Set<Integer>> methodMap = new HashMap<>();
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

                int hash = myHashCode(sb.toString());
                readInstrForMethod(lineSplit, methodMap, hash);
                instrMapping.put(hash, methodMap);
            }
            bf.close();
            inputReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return instrMapping;
    }

    public static Map<String, Map<VariableTrimInfo, Set<Integer>>> readInstrMapping_D4j(Configuration config) {
        Map<String, Map<VariableTrimInfo, Set<Integer>>> map = new HashMap<>();
        File file = new File(Configuration.instrMapping + "/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + ".txt");
        InputStreamReader inputReader;
        try {
            inputReader = new InputStreamReader(Files.newInputStream(file.toPath()));
            BufferedReader bf = new BufferedReader(inputReader);
            String line;
            String[] lineSplit;
            while ((line = bf.readLine()) != null) {
                Map<VariableTrimInfo, Set<Integer>> methodMap = new HashMap<>();
                lineSplit = line.split("\t");
                String className = lineSplit[0];
                int methodHash = Integer.parseInt(lineSplit[2]);
                readInstrForMethod(lineSplit, methodMap, methodHash);
                Map<VariableTrimInfo, Set<Integer>> subMap = map.get(className);
                if (subMap == null) {
                    subMap = new HashMap<>(methodMap);
                    map.put(className, subMap);
                } else
                    subMap.putAll(methodMap);
            }
            bf.close();
            inputReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    private static void readInstrForMethod(String[] lineSplit, Map<VariableTrimInfo, Set<Integer>> methodMap, int methodHash) {
        for (int i = 3; i < lineSplit.length; i++) {
            String[] varNames = lineSplit[i].split(" ");
            for (int j = 1; j < varNames.length; j++) {
                String nameDesc = varNames[j];
                if (nameDesc.contains("Exception"))
                    continue;
                int rightBrackets = nameDesc.indexOf(")");
                int second = nameDesc.indexOf("@");
                if (second == -1)
                    second = nameDesc.indexOf(":");
                String name = nameDesc.substring(rightBrackets + 1, second);
                if (name.contains("$"))
                    continue;
                int lineNum = Integer.parseInt(nameDesc.substring(second + 1));
                if (lineNum == -1)
                    continue;
                VariableInfo var = new VariableInfo(varNames[j], methodHash, lineSplit[0], lineSplit[1]);
                VariableTrimInfo varTrim = new VariableTrimInfo(var);
                Set<Integer> lines = methodMap.get(varTrim);
                if (lines == null) {
                    lines = new HashSet<>();
                    lines.add(lineNum);
                    methodMap.put(varTrim, lines);
                } else {
                    lines.add(lineNum);
                }
            }
        }
    }

    public static Map<TestSuite, Map<Integer, Integer[]>> readFailingBBAccess(Configuration config, RunCommand cmd) {
        String failingDir = Configuration.ReportRoot + "/failing/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + "/";
        return readBBAccess(failingDir, cmd.failing);
    }

    public static Map<TestSuite, Map<Integer, Integer[]>> readPassingBBAccess(Configuration config, RunCommand cmd) {
        String failingDir = Configuration.ReportRoot + "/passing/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + "/";
        return readBBAccess(failingDir, cmd.passing);
    }

    private static Map<Integer, Integer[]> readSingleTest(String folderPath, TestSuite suite) {
        Map<Integer, Integer[]> basicBlockAccess = new HashMap<>();
        try {
            File file = new File(folderPath + suite.className + "#" + suite.methodName + ".txt");
            if (!file.exists())
                return null;
            InputStreamReader inputReader = new InputStreamReader(Files.newInputStream(file.toPath()));
            BufferedReader bf = new BufferedReader(inputReader);
            String line;
            while ((line = bf.readLine()) != null) {
                String[] frame = line.split("\t");
                Integer[] in = new Integer[frame.length - 1];
                for (int i = 1; i < frame.length; i++)
                    in[i - 1] = Integer.parseInt(frame[i]);
                basicBlockAccess.put(Integer.parseInt(frame[0]), in);
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
        return basicBlockAccess;
    }

    public static int myHashCode(String str) {
        int hash = 0;
        int itr = str.length() / 32;
        if (str.length() % 32 != 0)
            itr += 1;
        for (int i = 0; i < itr; i++) {
            if (i != itr - 1)
                hash += str.substring(32 * i, 32 * i + 32).hashCode();
            else hash += str.substring(32 * i).hashCode();
        }
        return hash;
    }

    public static void writeMutants(String path, SootClass clazz) {
        try {
            int java_version = Options.v().java_version();
            OutputStream streamOut = Files.newOutputStream(Paths.get(path));
            BafASMBackend backend = new BafASMBackend(clazz, java_version);
            backend.generateClassFile(streamOut);
            streamOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void mkdirs(String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (!parent.isDirectory())
            parent.mkdirs();
    }

    public void doInstr() {
//        System.out.println("instrument the method to get all variables are involved which BBs");
        logger.info(String.format("instrument the method to get all variables are involved which BBs, mapping file is at %s"
                , Configuration.instrMappingRoot + "/" + config.getProjectName() + "/" +
                        config.getProjectName() + "_" + config.getCurrent() + ".txt"));
        if (MainClass.isLinux())
            runDefects4jCompileAndTest();
        File instrDir = new File(config.getProjectInstrPath());
        if (!instrDir.exists()) {
            instrDir.mkdirs();
        }
        Map<MethodInfo, Set<VariableInfo>[]> methodMap = new HashMap<>();
        initializeSoot();
        analyze(config.getProjectTargetPath(), "", methodMap);
        copyInstrumentClass(config.getProjectInstrPath());
        writeInstrMap(config, methodMap);
    }

    private void initializeSoot() {
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().setPhaseOption("jb.cp", "enabled:false");
        Options.v().set_write_local_annotations(true);
        Options.v().set_ignore_resolving_levels(true);
//        String sootClassPath = Scene.v().defaultClassPath() + ";D:/target_classes/Time/Time_1_buggy/target/classes/;target/classes/";
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

    public void analyzeForFailingAcc() {
        PackManager.v().getPack("jtp").add(
                new Transform("jtp.failing", new FailingAccessTransForm()));
        String[] sootArgs = new String[]{
                "-p", "jb", "use-original-names:true",
        };
        Main.main(sootArgs);
    }

    private void analyze(String path, String relative, Map<MethodInfo, Set<VariableInfo>[]> methodMap) {
        try {
            File directory = new File(path);
            if (!directory.isDirectory())
                throw new FileNotFoundException('"' + path + '"' + " input path is " +
                        "not a Directory , please input the right path of the Directory" +
                        ". ^_^...^_^");

            File[] fileList = directory.listFiles();
            assert fileList != null;
            for (File file : fileList) {
                if (file.isDirectory()) {
                    analyze(file.getAbsolutePath(), relative + "/" + file.getName(), methodMap);
                } else if (!file.getName().endsWith(".class")) {
                    String descPath;
                    path = path.replace("\\", "/");
                    if (path.contains("target/classes")) {
                        descPath = path.replaceAll("target/classes", "target/instr_classes");
                    } else {
                        descPath = path.replace("build", "instr_build");
                    }
                    FileUtils.copyFile(file, new File(descPath + File.separator + file.getName()));
                } else {
                    String clazzPath = relative.replace("/", ".").substring(1) + "." +
                            file.getName().substring(0, file.getName().indexOf(".class"));
                    SootClass clazz = Scene.v().loadClassAndSupport(clazzPath);
                    for (SootMethod method : clazz.getMethods()) {
                        if (method.isAbstract())
                            continue;
                        InstrTransform.internalTransform(method.retrieveActiveBody(), methodMap);
                    }
                    String writePath = config.getProjectInstrPath() + "/" +
                            clazzPath.replace(".", "/") + ".class";
                    mkdirs(writePath);
                    writeMutants(writePath, clazz);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String byteCodeSunSignature(SootMethod method) {
        String sig = method.getBytecodeSignature();
        int colon = sig.indexOf(":");
        return sig.substring(colon + 2, sig.length() - 1);
    }

    private void copyInstrumentClass(String instrPath) {
        try {
            FileUtils.copyFile(new File("target/classes/Counter.class"),
                    new File(instrPath + File.separator + "Counter.class"));
            FileUtils.copyFile(new File("target/classes/MyRunListener.class"),
                    new File(instrPath + File.separator + "MyRunListener.class"));
//            FileUtils.copyFile(new File("target/classes/myBlockJUnit4ClassRunner.class"),
//                    new File(instrPath + File.separator + "myBlockJUnit4ClassRunner.class"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeInstrMap(Configuration config, Map<MethodInfo, Set<VariableInfo>[]> methodMap) {
        File file = new File(Configuration.instrMappingRoot + File.separator +
                config.getProjectName() + File.separator +
                config.getProjectName() + "_" + config.getCurrent() + ".txt");
        File parent = file.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
        try {
            FileWriter fw = new FileWriter(file);
            for (Map.Entry<MethodInfo, Set<VariableInfo>[]> entry : methodMap.entrySet()) {
                MethodInfo methodInfo = entry.getKey();
                fw.write(methodInfo + "\t");
                Set<VariableInfo>[] BBs = entry.getValue();
                for (int i = 0; i < BBs.length; i++) {
                    Set<VariableInfo> locals = BBs[i];
                    fw.write(i + " ");
                    for (VariableInfo local : locals) {
                        if (!local.isDef && local.getLine() == -1)
                            continue;
//                        if (local.isArrayRef)
//                            fw.write("[]");
                        fw.write("(" + local.getDesc() + ")");
                        if (local.getLine() != -1 && !local.name.contains(".")) {
                            Pair<Integer, Boolean> info = getVarsInfo(local, BBs);
                            String clazzRoot = config.getProjectTargetPath();
                            String path = clazzRoot + methodInfo.className.replace(".", "/") + ".class";
                            String newName = recoverName(path, methodInfo.subSignature, local.desc, info);
                            if (newName == null) {
                                local.trueName = local.name;
//                                newName = recoverName(path, methodInfo.subSignature, local.desc, info);
//                                info = getVarsInfo(local, BBs);
                            } else
                                local.trueName = newName;
                        } else
                            local.trueName = local.name;

                        fw.write(local.name);
                        if (local.trueName != null)
                            fw.write("^" + local.trueName);
                        if (local.isDef()) {
                            fw.write("@");
                        } else {
                            fw.write(":");
                        }
                        fw.write(local.getLine() + " ");
                    }
                    fw.write("\t");

                }
                fw.write("\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String recoverName(String classPath, String methodSig, String desc, Pair<Integer, Boolean> info) {
        try {
            ClassNode cn = new ClassNode(ASM7);
            ClassReader cr = new ClassReader(Files.newInputStream(Paths.get(classPath)));
            cr.accept(cn, ClassReader.EXPAND_FRAMES | ClassReader.SKIP_FRAMES);
            int line = info.getKey();
            boolean isDef = info.getValue();
            Pair<String, Integer> newDesc = transformVarDescToBytecode(desc, isDef);
            for (MethodNode mn : cn.methods) {
                if (methodSig.equals(mn.name + mn.desc)) {
                    int currentLine = -1;
                    AbstractInsnNode[] instructions = mn.instructions.toArray();
                    List<LocalVariableNode> candidates = new ArrayList<>();
                    Map<LabelNode, Integer> labelIndexMap = new HashMap<>();
                    int targetInsnIndex = -1;
                    int targetVarIndex = -1;
                    for (int i = 0; i < mn.instructions.size(); i++) {
                        AbstractInsnNode ins = instructions[i];
                        if (ins instanceof LineNumberNode) {
                            LineNumberNode lineNumberNode = (LineNumberNode) ins;
                            currentLine = lineNumberNode.line;
                        } else if (ins instanceof LabelNode) {
                            labelIndexMap.put((LabelNode) ins, i);
                        } else if (line == currentLine) {
                            int opcode = -1;
                            if (instructions[i] instanceof VarInsnNode) {
                                VarInsnNode varInsnNode = (VarInsnNode) ins;
                                targetVarIndex = varInsnNode.var;
                                opcode = varInsnNode.getOpcode();
                            } else if (instructions[i] instanceof IincInsnNode) {
                                IincInsnNode node = (IincInsnNode) ins;
                                targetVarIndex = node.var;
                                opcode = node.getOpcode();
                            }
                            if (opcode == newDesc.getValue() || opcode == IINC) {
                                targetInsnIndex = i + 1;
                                for (LocalVariableNode local : mn.localVariables) {
                                    if (local.index == targetVarIndex &&
                                            local.desc.equals(newDesc.getKey())) {
                                        candidates.add(local);
                                    }
                                }
                            }
                        }

                    }
                    if (candidates.size() == 1) {
                        return candidates.get(0).name;
                    } else {
                        for (LocalVariableNode candidate : candidates) {
                            if (candidate.index == targetVarIndex &&
                                    labelIndexMap.get(candidate.start) <= targetInsnIndex &&
                                    labelIndexMap.get(candidate.end) >= targetInsnIndex) {
                                return candidate.name;
                            }
                        }
                    }
                    // may overwrite parameter slot
                    for (LocalVariableNode local : mn.localVariables) {
                        if (targetVarIndex == local.index) {
                            return local.name;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Pair<Integer, Boolean> getVarsInfo(VariableInfo local, Set<VariableInfo>[] bbMappings) {
        if (local.isDef)
            return new Pair<>(local.getLine(), true);
        int def = -1;
        int line = local.getLine();
        String name = local.name;
        for (Set<VariableInfo> bbMapping : bbMappings) {
            for (VariableInfo var : bbMapping) {
                if (var.name.equals(name) && var.isDef() && var.getLine() < line) {
                    if (var.getLine() > def)
                        def = var.getLine();
                }
            }
        }
        if (def == -1)
            return new Pair<>(local.getLine(), false);
        return new Pair<>(def, true);
    }


    private static Pair<String, Integer> transformVarDescToBytecode(String desc, boolean isDef) {
        StringBuilder prefix = new StringBuilder();
        int store = -1;
        int load = -1;
        while (desc.endsWith("[]")) {
            desc = desc.substring(0, desc.length() - 2);
            prefix.append("[");
            store = ASTORE;
            load = ALOAD;
        }
        switch (desc) {
            case "boolean":
                if (store == -1)
                    store = ISTORE;
                if (load == -1)
                    load = ILOAD;
                if (isDef) return new Pair<>(prefix + "Z", store);
                else return new Pair<>(prefix + "Z", load);
            case "char":
                if (store == -1)
                    store = ISTORE;
                if (load == -1)
                    load = ILOAD;
                if (isDef) return new Pair<>(prefix + "C", store);
                else return new Pair<>(prefix + "C", load);
            case "byte":
                if (store == -1)
                    store = ISTORE;
                if (load == -1)
                    load = ILOAD;
                if (isDef) return new Pair<>(prefix + "B", store);
                else return new Pair<>(prefix + "B", load);
            case "short":
                if (store == -1)
                    store = ISTORE;
                if (load == -1)
                    load = ILOAD;
                if (isDef) return new Pair<>(prefix + "S", store);
                else return new Pair<>(prefix + "S", load);
            case "int":
                if (store == -1)
                    store = ISTORE;
                if (load == -1)
                    load = ILOAD;
                if (isDef) return new Pair<>(prefix + "I", store);
                else return new Pair<>(prefix + "I", load);
            case "float":
                if (store == -1)
                    store = FSTORE;
                if (load == -1)
                    load = FLOAD;
                if (isDef) return new Pair<>(prefix + "F", store);
                else return new Pair<>(prefix + "F", load);
            case "double":
                if (store == -1)
                    store = DSTORE;
                if (load == -1)
                    load = DLOAD;
                if (isDef) return new Pair<>(prefix + "D", store);
                else return new Pair<>(prefix + "D", load);
            case "long":
                if (store == -1)
                    store = LSTORE;
                if (load == -1)
                    load = LLOAD;
                if (isDef) return new Pair<>(prefix + "J", store);
                else return new Pair<>(prefix + "J", load);
            default:
                if (isDef) return new Pair<>(prefix + "L" + desc.replace(".", "/") + ";", ASTORE);
                else return new Pair<>(prefix + "L" + desc.replace(".", "/") + ";", ALOAD);
        }
    }

    public void runDefects4jCompileAndTest() {
        try {
//            File workDir = new File(config.getProjectRoot());
            File file = new File(config.getProjectRoot() + "/cmd.sh");

            FileWriter fw = new FileWriter(file);

            fw.write("cd " + config.getProjectRoot() + "\n");
            fw.write("echo running defects4j export -p tests.all -o alltest\n");
            fw.write("defects4j export -p tests.all -o alltest\n");
            fw.write("echo running defects4j export -p cp.test -o dep\n");
            fw.write("defects4j export -p cp.test -o dep\n");
            fw.write("echo running defects4j export -p tests.trigger -o trigger\n");
            fw.write("defects4j export -p tests.trigger -o trigger\n");
            fw.write("echo running defects4j compile\n");
            fw.write("defects4j compile\n");
            fw.close();

            Process p = new ProcessBuilder().command("bash", config.getProjectRoot() + "/cmd.sh").start();

            BufferedInputStream in = new BufferedInputStream(p.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
            in.close();
            file.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private Map<MethodInfo, Set<VariableInfo>[]> analyze() {
//        Map<MethodInfo, Set<VariableInfo>[]> methodMap = new HashMap<>();
//        PackManager.v().getPack("jtp").add(
//                new Transform("jtp.instr" + config.getCurrent(), new InstrTransform(methodMap)));
//        String[] args = new String[]{
//                "-p","jb","use-original-names:true"
//        };
//        Main.main(args);
////        PackManager.v().runPacks();
////        PackManager.v().writeOutput();
//        return methodMap;
//    }
}
