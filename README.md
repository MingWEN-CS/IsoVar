# IsoVar replication package

## About

### Goal
IsoVar is designed to isolate fault-correlated variables via integrating the statistical analysis and mutation analysis. It can also be applied to existing FL or APR tools to enhance their performance. 

### Input/Output
**input**
- Root path of project under test.
- Target directory of classes.
- Target directory of test classes.
- List of failing cases and passing cases.
- Dependency path to run tests.

**output**
- List of suspicious variables, sorted by their suspicious values.

### How

IsoVar first instrument the project under test and records information about the variables in each basic block, 
including variable name, variable type, def or use and linenumber based on [Soot](https://github.com/soot-oss/soot).
Based on our two intuitions: (1) the variable should be more frequently involved in failing execution traces while less frequently involved in passing execution traces.
(2) Mutating the correlated variables will cast higher impact on failing executions while less impact on passing ones, and vice versa for those irrelevant variables.

IsoVar implements the above intuitions by combining the statistical analysis phase and mutation analysis phase and locates the fault-related variables among all the variables on the failing test path.

## Prerequisites

Note: all the experiments were conducted over a server equipped with 128 GB RAM, 2.3 GHz on 24 cores and 64-bit Ubuntu system.
We strongly recommend a similar or better hardware environment.
The operating system however could be changed.

**Softwares**
1. [Java 1.8](https://www.oracle.com/java/technologies/downloads/#java8)  or higher
2. [Defects4j](https://github.com/rjust/defects4j). We provide a demo without installing D4j if you don't want to install it.
3. [Bears](https://github.com/bears-bugs/bears-benchmark).

## Running IsoVar in a general way
1. checkout a project from Defects4j or Bears
```
# For Defects4j
$ defects4j checkout -p Time -v 1b -w path_to_the_prject
$ cd path_to_the_prject

# compile the project. 
$ defects4j compile

# get List of test methods that trigger the bug
$ defects4j export -p tests.trigger -o trigger

# get list of all developer-written test classes
$ defects4j export -p tests.all -o alltest

# get dependency path to run tests.(IsoVar rely on it to run tests)
$ defects4j export -p cp.test -o dep

# For Bears
$ python scripts/checkout_bug.py --bugId <bug ID> --workspace path_to_the_prject
$ cd path_to_the_prject

# compile
mvn compile

# get dependencies path to run tests.
$ mvn dependency:build-classpath -D"mdep.outputFile=dep"
```
2. clone this project, the artifacts is located at [artifacts/IsoVar.jar](artifacts/IsoVar.jar).
```
$ cd IsoVar
$ java -jar .\artifacts\IsoVar.jar --help
usage: java -jar IsoVar.jar [OPTIONS]
 -?,--help              print this help message
 --alpha <arg>           value of gamma, default is 0.4
 --beta <arg>            value of beta, default is 0.9
 --gamma <arg>            value of beta, default is 1
 --binary <arg>          target directory of classes (relative to working
                        directory)
 --debug <arg>           enable debug? default is false.
 --dependency <arg>      path to all dependency, eg, path_to_dependency/*
                        or a file containing all dependency.(relative to
                        working directory)
 --ID <arg>              bug id
 --max_mutation <arg>    max mutation time for one variables. default is
                        10.
 --phase <arg>           analyze phase, [instrument] or [analyze] or [parameters_tuning]
 --project_name <arg>    project name
 --project_root <arg>    target directory of project path
 --report_dir <arg>      report dir path. default is
                        report/projectName/projectName_ID.txt
 --skip_mutation <arg>   skip muation phase? default is false
 --test_binary <arg>     Target directory of test classes (relative to
                        working directory)
 --timeout <arg>         timeout for one test classes. default is 30s.

```

3. instrument the project and a mapping file will be generated at [InstrMapping/Time/Time_1.txt](InstrMapping/Time/Time_1.txt)
```
$ java -jar artifacts/IsoVar.jar --phase instrument --project_name Time --ID 1
```
Note: for Defects4j and Bears, we have hard-coded some information related to the two benchmarks, including binary path, test binary path, number of bugs.

4. isolate suspicious variables list and the file will be generated at [report/Time/Time_1.txt](report/Time/Time_1.txt)
```
$ java -jar artifacts/IsoVar.jar --phase analyze --project_name Time --ID 1
```

the format for each variable is: className \t methodSignature \t variableName \t suspicious  


## Running IsoVar on a demo without installing Defect4j: demo.zip
1. unzip the demo.zip and there are two directory: Time_1_buggy and Time1_dependency
2. instrument
```
java -jar artifacts/IsoVar.jar --phase instrument --project_name Time --ID 1 --project_root path/to/Time_1_buggy 
```
3. analyze and the report will be generated at [report/Time/Time_1.txt](report/Time/Time_1.txt)
```
java -jar artifacts/IsoVar.jar --phase analyze --project_name Time --ID 1 --dependency path/to/Time1_dependency/ --project_root path/to/Time_1_buggy
```
4. auto-tune the parameters (alpha, beta and gamma). The program will read the report and utilize the intermediate results to find optimal parameters setting.
```
java -jar artifacts/IsoVar.jar --phase parameters_tuning
```

# result analyze

- The top 5 variables with suspicious values are: 
```
org.joda.time.field.UnsupportedDurationField	compareTo(Lorg/joda/time/DurationField;)I	durationField	0.4985170538803757
org.joda.time.Partial	<init>([Lorg/joda/time/DateTimeFieldType;[ILorg/joda/time/Chronology;)V	i	0.4933133236564228
org.joda.time.Partial	<init>([Lorg/joda/time/DateTimeFieldType;[ILorg/joda/time/Chronology;)V	compare	0.48525960692569586
org.joda.time.Partial	<init>([Lorg/joda/time/DateTimeFieldType;[ILorg/joda/time/Chronology;)V	this.iChronology	0.48207900331000014
org.joda.time.Partial	<init>([Lorg/joda/time/DateTimeFieldType;[I)V	types	0.48153547133138963
```

the newest version will also print some intermediate results, you can ignore them

- the patch of Time 1 is https://github.com/rjust/defects4j/blob/master/framework/projects/Time/patches/1.src.patch

Note: The addition and deletion of lines in this patch are reversed. Specifically, the "+" in the real patch is in fact a deletion of a line. "-" in a real patch is in fact adding a line.

- the oracle variables of Time 1 are:
```
buggy	org.joda.time.field.UnsupportedDurationField	227	DEL	durationField
fix	org.joda.time.Partial	217	INS	loopUnitField
fix	org.joda.time.Partial	230	INS	compare
fix	org.joda.time.Partial	230	INS	lastUnitField
buggy	org.joda.time.Partial	221	MOV	compare
fix	org.joda.time.Partial	218	INS	lastUnitField
fix	org.joda.time.Partial	220	INS	loopType
fix	org.joda.time.Partial	223	INS	loopType
fix	org.joda.time.Partial	220	INS	types
fix	org.joda.time.Partial	220	INS	i
fix	org.joda.time.Partial	223	INS	types
fix	org.joda.time.Partial	223	INS	i
```

so IsoVar can rank oracle variables "durationField", "i", "compare" and "types" as 1,2,3,5


