package Similarity;

import Instrument.BBMapping;
import IsoVar.Configuration;
import IsoVar.VariableInfo;
import IsoVar.VariableTrimInfo;
import Util.RunCommand;
import Util.TestSuite;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static Instrument.Instrument.myHashCode;

public class Statistical {
    private final int maxVarCount = 60;
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
                if (tmp.get(var.trueName) != null) {
                    Set<Integer> in = tmp.get(var.trueName);
                    in.add(mapping.index);
                } else if (!var.isParameter) {
                    Set<Integer> in = new HashSet<>();
                    in.add(mapping.index);
                    tmp.put(var.trueName, in);
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
                    Set<Integer> in = tmp.get(var.trueName);
                    if (in != null && in.contains(mapping.index)) {
                        if (!addedVar.contains(var.trueName)) {

                            varsTrim.putIfAbsent(new VariableTrimInfo(var), in);
                            addedVar.add(var.trueName);
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
            VariableTrimInfo varT = entry.getKey();
            VariableInfo var = varT.var;
            Set<Integer> varMappingBB = entry.getValue();
            List<Integer[]> varFailingAccess = new ArrayList<>();
            double failingFreq = calFrequency(varMappingBB, failingBBAccess,
                    varFailingAccess, var.methodHash, varT.var.isClinit);
            List<Integer[]> varPassingAccess = new ArrayList<>();
            double passingFreq = calFrequency(varMappingBB, passingBBAccess,
                    varPassingAccess, var.methodHash, varT.var.isClinit);
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
                varT.statistical = failingFreq / (failingFreq + passingFreq) - cosSim * this.alpha;
                if (varT.statistical > -0.001 && varT.statistical < 0.001)
                    varT.statistical = 0;
            } else
                varT.statistical = 0;

            varT.failingFreq = failingFreq;
            varT.passingFreq = passingFreq;
            varT.cosSim = cosSim;

            varT.suspicious += varT.statistical;
        }
    }

    public void writeStatisticalResult(Configuration config, Map<Integer, BBMapping[]> bbMappings) {
        try {
//            File file = new File(config.similaritiesRoot+"_allmethod" + "/" + config.getProjectName() + "/" +
            File file = new File(config.getReport_dir() + "/" + config.getProjectName() + "/" +
                    config.getProjectName() + "_" + config.getCurrent() + ".txt");
            file.getParentFile().mkdirs();
            FileWriter fw = new FileWriter(file);
            List<VariableTrimInfo> sortedVars = new ArrayList<>(this.vars.keySet());
            sortedVars.sort((v1, v2) -> Double.compare(v2.suspicious, v1.suspicious));
            for (VariableTrimInfo varT : sortedVars) {
                VariableInfo var = varT.var;
//                String name = var.trueName == null ? var.name : var.trueName;
//                fw.write(var.methodHash + "\t" + name + "\t" + var.suspicious +
//                        "\t[" + var.statistical + " " + var.failingFreq + " " + var.passingFreq + " " + var.cosSim +
//                        "]\t[" + var.mutate + " " + var.failingDiff + " " + var.passingDiff + "]\n");
                fw.write(var.className + "\t" + var.methodName + "\t" + var.trueName + "\t" + varT.suspicious +
                        "\t[" + varT.statistical + " " + varT.failingFreq + " " + varT.passingFreq + " " + varT.cosSim +
                        "]\t[" + varT.mutate + " " + varT.failingDiff + " " + varT.passingDiff + "]\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Double> readStatisticalResult(Configuration config) {
        Map<String, Double> varSusMap = new HashMap<>();
        File file = new File(Configuration.ReportRoot + "/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + ".txt");
        InputStreamReader ir;
        try {
            ir = new InputStreamReader(Files.newInputStream(file.toPath()));
            BufferedReader bf = new BufferedReader(ir);
            String line;
            String[] split;
            while ((line = bf.readLine()) != null) {
                split = line.split("\t");
                String className = split[0];
                String methodSig = split[1];
                String varName = split[2];
//                double suspicious = Double.parseDouble(split[3]);
                String[] statStr = split[4].split(" ");
                double failFreq = Double.parseDouble(statStr[1]);
                double passFreq = Double.parseDouble(statStr[2]);
                if (passFreq > -0.001 && passFreq < 0.001)
                    passFreq = 0;
                double cosSim = Double.parseDouble(statStr[3].substring(0, statStr[3].length() - 1));
                double stat = failFreq / (failFreq + passFreq) - cosSim * 0.5;
//                if (cosSim == 0)
//                    stat = 0;
                if (cosSim == 0 && config.getProjectName().equals("Math"))
                    stat = 1;

                String[] mutateStr = split[5].split(" ");
                double failingDiff = Double.parseDouble(mutateStr[1]);
                double passingDiff = Double.parseDouble(mutateStr[2].substring(0, mutateStr[2].length() - 1));
                double mutate = failingDiff - passingDiff * config.getBeta();

                double suspicious;
                if (mutate == 0)
                    suspicious = stat;
                else
                    suspicious = stat + mutate * config.getGamma();
                int hash = myHashCode(className) + myHashCode(methodSig);
                String id = hash + "#" + varName;
                varSusMap.put(id, suspicious);
            }
            bf.close();
            ir.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return varSusMap;
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
        for (VariableTrimInfo varTrim : varsToBeMutated) {
            Set<VariableInfo> toAddVars = new HashSet<>();
            BBMapping[] BBs = bbMappings.get(varTrim.var.methodHash);
            Integer[] access = suite.get(varTrim.var.methodHash);
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
                    if (!var.name.equals(varTrim.var.name))
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
                    if (!var.name.equals(varTrim.var.name))
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
            if (map.containsKey(varTrim.var.methodHash))
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
            map.put(varTrim.var.methodHash, set);
        }
        return map;
    }

    private String findClasses(Map<TestSuite, Map<Integer, Integer[]>> BBAccess, VariableTrimInfo varTrim) {
        List<String> list2 = new ArrayList<>();
        for (Map.Entry<TestSuite, Map<Integer, Integer[]>> entry : BBAccess.entrySet()) {
            Map<Integer, Integer[]> access = entry.getValue();
            if (access.get(varTrim.var.methodHash) != null) {
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