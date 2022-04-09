package Similarity;

import Instrument.BBMapping;
import IsoVar.Configuration;
import IsoVar.VariableInfo;
import IsoVar.VariableTrimInfo;
import Util.Pair;
import Util.RunCommand;
import Util.TestSuite;
import org.javatuples.Quartet;
import org.javatuples.Triplet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

public class Statistical {
    private final int maxVarCount = 40;
    public Map<VariableTrimInfo, Set<Integer>> vars;
    double alpha;


    public Statistical(Map<Integer, BBMapping[]> bbMappings,
                       Map<TestSuite, Map<Integer, Integer[]>> failingBBAccess,
                       Configuration config) {
        this.vars = identifySuspiciousVars(bbMappings, failingBBAccess.values());
        this.alpha = config.getAlpha();
    }

    public static double calSingleCosSimilarity(Integer[] in1, Integer[] in2) {
        if (in1.length != in2.length) {
            System.err.println("the Integer array length is not equal");
            System.exit(0);
        }
        double numerator = 0;
        double vector1Modulo = 0;
        double vector2Modulo = 0;
        for (int i = 0; i < in1.length; i++) {
            numerator += (double) in1[i] * (double) in2[i];
            vector1Modulo += Math.pow(in1[i], 2);
            vector2Modulo += Math.pow(in2[i], 2);
        }
        vector1Modulo = Math.sqrt(vector1Modulo);
        vector2Modulo = Math.sqrt(vector2Modulo);
        if (vector1Modulo * vector2Modulo == 0)
            return 0;
        return numerator / (vector1Modulo * vector2Modulo);
    }

    private Map<VariableTrimInfo, Set<Integer>> identifySuspiciousVars(Map<Integer, BBMapping[]> bbMappings,
                                                                       Collection<Map<Integer, Integer[]>> values) {
        Map<VariableTrimInfo, Set<Integer>> vars = new HashMap<>();
        if (values.isEmpty()) {
            System.err.println("failing test may timeout");
            System.exit(0);
        }
        Iterator<Map<Integer, Integer[]>> it = values.iterator();
        Map<Integer, Integer[]> first = it.next();
        Set<Integer> common = new HashSet<>();
//        Set<Integer> common = new HashSet<>(first.keySet());
//        while (it.hasNext()) {
//            common.retainAll(it.next().keySet());
//        }
        for (Map<Integer, Integer[]> value : values) {
            common.addAll(value.keySet());
        }

        for (Integer key : common) {
            BBMapping[] mappings = bbMappings.get(key);
            boolean[] accessed = new boolean[mappings.length];
            Arrays.fill(accessed, false);
            for (Map<Integer, Integer[]> maps : values) {
                Integer[] exec = maps.get(key);
                if (exec == null)
                    continue;
                for (int i = 0; i < exec.length; i++) {
                    if (exec[i] != 0)
                        accessed[i] = true;
                }
            }
            Map<VariableTrimInfo, Set<Integer>> susVars = this.suspiciousVarInMethod(mappings, accessed);
            vars.putAll(susVars);
        }
        return vars;
    }

    private Map<VariableTrimInfo, Set<Integer>> suspiciousVarInMethod(BBMapping[] mappings, boolean[] accessed) {
        Map<String, Set<Integer>> tmp = new HashMap<>();
        Map<VariableTrimInfo, Set<Integer>> varsTrim = new HashMap<>();

        for (BBMapping mapping : mappings) {
            for (VariableInfo var : mapping.vars) {
                if (var.isParameter)
                    continue;
//                VariableTrimInfo trimVar = new VariableTrimInfo(var.name, var.desc, var.methodHash, var.isClinit);
                if (tmp.get(var.name) != null) {
                    Set<Integer> in = tmp.get(var.name);
                    in.add(mapping.index);
                } else if (!var.isParameter) {
                    Set<Integer> in = new HashSet<>();
                    in.add(mapping.index);
                    tmp.put(var.name, in);
                }
            }
        }

        // get vars in a method and which BBs it is involved
        // for the parameter vars, if one of them used at least in one bb, then we mutate it
        Set<String> addedVar = new HashSet<>();
        for (int i = 0; i < accessed.length; i++) {
            if (accessed[i]) {
                BBMapping mapping = mappings[i];
                for (VariableInfo var : mapping.vars) {
                    Set<Integer> in = tmp.get(var.name);
                    if (in != null && in.contains(mapping.index)) {
                        if (!addedVar.contains(var.name)) {
                            varsTrim.putIfAbsent(new VariableTrimInfo(var.name, var.desc, var.methodHash, var.isClinit), in);
                            addedVar.add(var.name);
                        }
                    }
                }
            }
        }
        return varsTrim;
    }

    private double calFrequency(Set<Integer> varMappingBB,
                                Map<TestSuite, Map<Integer, Integer[]>> BBAccessMap,
                                List<Integer[]> varAccess,
                                int methodHash, boolean isClinit) {
        double res = 0;
        List<String> classNameIfClinit = new ArrayList<>();
        for (Map.Entry<TestSuite, Map<Integer, Integer[]>> entry : BBAccessMap.entrySet()) {
            TestSuite suite = entry.getKey();
            Map<Integer, Integer[]> accessMap = entry.getValue();
            Integer[] access = accessMap.get(methodHash);
            Integer[] in = new Integer[varMappingBB.size()];
            if (access != null) {
                int appear = 0;
                int index = 0;
                for (Integer i : varMappingBB) {
                    in[index++] = access[i];
                    if (access[i] != 0)
                        appear += 1;
                }
                if (appear != 0) {
                    res += (double) appear / (double) varMappingBB.size();
                    varAccess.add(in);
                    if (isClinit)
                        classNameIfClinit.add(suite.className);
                }
            } else {
                Arrays.fill(in, 0);
//                varAccess.add(in);
            }
        }
        if (BBAccessMap.size() != 0) {
            if (!isClinit)
                res /= BBAccessMap.size();
            else {
                int cnt = 0;
                for (TestSuite suite : BBAccessMap.keySet()) {
                    if (classNameIfClinit.contains(suite.className))
                        cnt++;
                }
                res = res * cnt / BBAccessMap.size();
            }
        } else res = 0;
        if (res < 0.0001 && res > -0.0001)
            res = 0.0001;
        return res;
    }

    public void doCalculate(Map<TestSuite, Map<Integer, Integer[]>> failingBBAccess,
                            Map<TestSuite, Map<Integer, Integer[]>> passingBBAccess) {
        for (Map.Entry<VariableTrimInfo, Set<Integer>> entry : this.vars.entrySet()) {
            VariableTrimInfo var = entry.getKey();
            Set<Integer> varMappingBB = entry.getValue();
            List<Integer[]> varFailingAccess = new ArrayList<>();
            double failingFreq = calFrequency(varMappingBB, failingBBAccess,
                    varFailingAccess, var.methodHash, var.isClinit);
            List<Integer[]> varPassingAccess = new ArrayList<>();
            double passingFreq = calFrequency(varMappingBB, passingBBAccess,
                    varPassingAccess, var.methodHash, var.isClinit);
            double avg = 0;
            for (Integer[] failingAccess : varFailingAccess) {
                for (Integer[] passingAccess : varPassingAccess) {
                    double sim = calSingleCosSimilarity(failingAccess, passingAccess);
                    avg += sim;
                }
            }
            double cosSim;
            if (varFailingAccess.size() * varPassingAccess.size() != 0 && (failingFreq + passingFreq) != 0) {
                cosSim = avg / (varFailingAccess.size() * varPassingAccess.size());
            } else cosSim = 0;

            if ((failingFreq + passingFreq) != 0) {
                var.statistical = failingFreq / (failingFreq + passingFreq) - cosSim * this.alpha;
                if (var.statistical > -0.001 && var.statistical < 0.001)
                    var.statistical = 0;
            } else
                var.statistical = 0;

            var.failingFreq = failingFreq;
            var.passingFreq = passingFreq;
            var.cosSim = cosSim;

            var.suspicious += var.statistical;
        }
    }

    public void writeStatisticalResult(Configuration config, Map<Integer, BBMapping[]> bbMappings) {
        try {
//            File file = new File(config.similaritiesRoot+"_allmethod" + "/" + config.getProjectName() + "/" +
            File file = new File(config.getReport_dir() + "/" + config.getProjectName() + "/" +
                    config.getProjectName() + "_" + config.getCurrent() + ".txt");
            File parent = file.getParentFile();
            if (!parent.exists())
                parent.mkdirs();
            FileWriter fw = new FileWriter(file);
            List<VariableTrimInfo> sortedVars = new ArrayList<>(this.vars.keySet());
            sortedVars.sort((v1, v2) -> Double.compare(v2.suspicious, v1.suspicious));
            String clazzRoot = config.getProjectTargetPath();
            for (VariableTrimInfo var : sortedVars) {
                String name = var.name;
                Quartet<String, String, Integer, Integer> varInfo = getVarsInfo(name, bbMappings.get(var.methodHash));
                if (varInfo == null)
                    continue;
                if (name.contains("#")) {
                    String newName = recoverName(var.desc, clazzRoot, varInfo);
                    if (newName != null)
                        name = newName;
                }
//                fw.write(var.methodHash + "\t" + name + "\t" + var.suspicious +
//                        "\t[" + var.statistical + " " + var.failingFreq + " " + var.passingFreq + " " + var.cosSim +
//                        "]\t[" + var.mutate + " " + var.failingDiff + " " + var.passingDiff + "]\n");
                fw.write(varInfo.getValue0() + "\t" + varInfo.getValue1() + "\t" + name + "\t" + var.suspicious + "\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String recoverName(String desc, String clazzRoot, Quartet<String, String, Integer, Integer> info) {
        try {
            String path = clazzRoot + info.getValue0().replace(".", "/") + ".class";
            ClassNode cn = new ClassNode(ASM7);
            ClassReader cr = new ClassReader(new FileInputStream(path));
            cr.accept(cn, ClassReader.EXPAND_FRAMES | ClassReader.SKIP_FRAMES);
            int def = info.getValue2();
            int use = info.getValue3();
            Pair<String, Integer> newDesc = transformVarDescToBytecode(desc);
            for (MethodNode mn : cn.methods) {
                if (info.getValue1().equals(mn.name + mn.desc)) {
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
                        } else if (def == currentLine && instructions[i] instanceof VarInsnNode) {
                            VarInsnNode varInsnNode = (VarInsnNode) ins;
                            targetVarIndex = varInsnNode.var;
                            int opcode = varInsnNode.getOpcode();
                            if (opcode == newDesc.getValue()) {
                                for (LocalVariableNode local : mn.localVariables) {
                                    if (labelIndexMap.containsKey(local.start))
                                        continue;
                                    if (local.index == targetVarIndex &&
                                            local.desc.equals(newDesc.getKey())) {
                                        candidates.add(local);
                                    }
                                }
                            }
                        } else if (use == currentLine && instructions[i] instanceof VarInsnNode) {
                            VarInsnNode varInsnNode = (VarInsnNode) ins;
                            if (varInsnNode.var == targetVarIndex)
                                targetInsnIndex = i;
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

    private Pair<String, Integer> transformVarDescToBytecode(String desc) {
        StringBuilder prefix = new StringBuilder();
        int store = -1;
        while (desc.endsWith("[]")) {
            desc = desc.substring(0, desc.length() - 2);
            prefix.append("[");
            store = ASTORE;
        }
        switch (desc) {
            case "boolean":
                if (store == -1)
                    store = ISTORE;
                return new Pair<>(prefix + "Z", store);
            case "char":
                if (store == -1)
                    store = ISTORE;
                return new Pair<>(prefix + "C", store);
            case "byte":
                if (store == -1)
                    store = ISTORE;
                return new Pair<>(prefix + "B", store);
            case "short":
                if (store == -1)
                    store = ISTORE;
                return new Pair<>(prefix + "S", store);
            case "int":
                if (store == -1)
                    store = ISTORE;
                return new Pair<>(prefix + "I", store);
            case "float":
                if (store == -1)
                    store = FSTORE;
                return new Pair<>(prefix + "F", store);
            case "double":
                if (store == -1)
                    store = DSTORE;
                return new Pair<>(prefix + "D", store);
            case "long":
                if (store == -1)
                    store = LSTORE;
                return new Pair<>(prefix + "J", store);
            default:
                return new Pair<>(prefix + "L" + desc.replace(".", "/") + ";", ASTORE);
        }
    }


    private Quartet<String, String, Integer, Integer> getVarsInfo(String name, BBMapping[] bbMappings) {
        int def = -1;
        int use = -1;
        VariableInfo tmp = null;
        for (BBMapping bbMapping : bbMappings) {
            if (def != -1 && use != -1)
                break;
            for (VariableInfo var : bbMapping.vars) {
                if (var.name.equals(name) && var.isDef() && def == -1) {
                    def = var.getLine();
                }
                if (var.name.equals(name) && !var.isDef() && use == -1) {
                    use = var.getLine();
                    tmp = var;
                }
            }
        }
        if (tmp == null)
            return null;
        return new Quartet<>(tmp.className, tmp.methodName, def, use);
    }


    public Map<VariableTrimInfo, Set<VariableInfo>> prepareVarsToBeMutated(Map<Integer, BBMapping[]> bbMappings,
                                                                           Map<TestSuite, Map<Integer, Integer[]>> failingBBAccess) {
        Map<VariableTrimInfo, Set<VariableInfo>> varsToMutate = new HashMap<>();
        Map<Integer, Integer[]> suite = new ArrayList<>(failingBBAccess.values()).get(0);
        List<VariableTrimInfo> sortedVars = new ArrayList<>(this.vars.keySet());
        sortedVars.sort((v1, v2) -> Double.compare(v2.statistical, v1.statistical));
        List<VariableTrimInfo> varsToBeMutated = new ArrayList<>();
        if (sortedVars.size() > maxVarCount) {
            for (VariableTrimInfo sortedVar : sortedVars) {
                if (!(sortedVar.passingFreq > -0.001 && sortedVar.passingFreq < 0.001)) {
                    varsToBeMutated.add(sortedVar);
                    if (varsToBeMutated.size() >= maxVarCount)
                        break;
                }
            }
        }
        for (VariableTrimInfo varTrim : sortedVars) {
            Set<VariableInfo> toAddVars = new HashSet<>();
            BBMapping[] BBs = bbMappings.get(varTrim.methodHash);
            Integer[] access = suite.get(varTrim.methodHash);
            if (access == null) {
                access = new Integer[bbMappings.size()];
                Arrays.fill(access, 0);
            }
            // a parameter, mutate it if it is used at least once
            // local, mutate it when it is defined/redifined in trace
            VariableInfo paramVar = null;
            for (BBMapping BB : BBs) {
                if (access[BB.index] == 0)
                    continue;
                // for defs in BB
                for (VariableInfo var : BB.vars) {
                    if (!var.name.equals(varTrim.name))
                        continue;
                    if (var.isDef()) {
                        if (var.isParameter)
                            paramVar = var;
                        else toAddVars.add(var);
                    } else if (var.isField) {  // static or instance field may not be defined
                        toAddVars.add(var);
                    }
                }

                // for uses in BB
                for (VariableInfo var : BB.vars) {
                    if (!var.name.equals(varTrim.name))
                        continue;
                    if (!var.isDef() && paramVar != null) {
                        toAddVars.add(paramVar);
                        paramVar = null;
                    }
                }
            }
//            if
//                System.out.println(varTrim.methodHash + " " + varTrim.name);
            if (!toAddVars.isEmpty())
                varsToMutate.put(varTrim, toAddVars);
        }
        return varsToMutate;
    }

    public Map<Integer, Set<String>> shrinkPassingForMutatePhase(Map<TestSuite, Map<Integer, Integer[]>> failingBBAccess,
                                                                 Map<TestSuite, Map<Integer, Integer[]>> passingBBAccess,
                                                                 Set<VariableTrimInfo> varTrims,
                                                                 RunCommand cmd) {
        Map<Integer, Set<String>> map = new HashMap<>();
        for (VariableTrimInfo varTrim : varTrims) {
            if (map.containsKey(varTrim.methodHash))
                continue;
            Set<String> set = new HashSet<>();
            String clazzForFailing = findClasses(failingBBAccess, varTrim);
            set.add(clazzForFailing);
            boolean existPassing = false;
            for (TestSuite passingSuite : cmd.passing) {
                if (passingSuite.className.equals(clazzForFailing)) {
                    existPassing = true;
                    break;
                }
            }
            if (!existPassing) {
                String clazz = findClasses(passingBBAccess, varTrim);
                if (clazz.equals("noM"))
                    continue;
                else
                    set.add(clazz);
            }
            map.put(varTrim.methodHash, set);
        }
        return map;
    }

    private String findClasses(Map<TestSuite, Map<Integer, Integer[]>> BBAccess, VariableTrimInfo varTrim) {
        List<String> list2 = new ArrayList<>();
        for (Map.Entry<TestSuite, Map<Integer, Integer[]>> entry : BBAccess.entrySet()) {
            Map<Integer, Integer[]> access = entry.getValue();
            if (access.get(varTrim.methodHash) != null) {
                TestSuite suite = entry.getKey();
                list2.add(suite.className);
            }
        }
        if (list2.isEmpty()) {
            return "noM";
//            System.err.println("do not exist a case that involve target method");
//            System.exit(0);
        }
        return findMaxOccur(list2);
    }

    private String findMaxOccur(List<String> list) {
        HashMap<String, Integer> hashMap = new HashMap<>(list.size());
        for (String c : list) {
            hashMap.put(c, hashMap.getOrDefault(c, 0) + 1);
        }
        List<Map.Entry<String, Integer>> entryList = hashMap.entrySet().stream().
                sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue())).collect(Collectors.toList());
        for (Map.Entry<String, Integer> entry : entryList) {
            return entry.getKey(); // return the first one
        }
        return null;
    }

}

class LocalVariable {
    String name;
    String desc;
    LabelNode start;
    LabelNode end;
    int index;

    public LocalVariable(String name, String desc, LabelNode start, LabelNode end, int index) {
        this.name = name;
        this.desc = desc;
        this.start = start;
        this.end = end;
        this.index = index;
    }
}