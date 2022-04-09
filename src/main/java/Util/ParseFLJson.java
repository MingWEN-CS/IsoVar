package Util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;


public class ParseFLJson {

    public static final String FL_base = "FL_baseline/";

    //读取json文件
    public static String readJsonFile(File jsonFile) {
        try {
            FileReader fileReader = new FileReader(jsonFile);
            Reader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8);
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void main(String[] args) {
        String path = "G:/FL_Baseline/data/release.json";
        File jsonFile = new File(path);
        String s = readJsonFile(jsonFile);
        JSONObject jobj = JSON.parseObject(s);

        try {
            for (Map.Entry<String, Object> entry : jobj.entrySet()) {
//            System.out.println(entry.getKey());
                String projectName = entry.getKey().split("\\d")[0];
                projectName = projectName.substring(0, 1).toUpperCase() + projectName.substring(1);
                String id = entry.getKey().split("\\D")[entry.getKey().split("\\D").length - 1];

                File file = new File( FL_base+"/"+ projectName + "/" + projectName + "_" + id + ".txt");
                FileWriter fw = new FileWriter(file);
                JSONObject subobj = (JSONObject) entry.getValue();
                for (Map.Entry<String, Object> subentry : subobj.entrySet()) {
//                    System.out.println("key值=" + subentry.getKey());
                    String className = subentry.getKey().split(":")[0];
                    String line = subentry.getKey().split(":")[1];
//                System.out.println("value="+subentry.getValue());
                    JSONObject array = (JSONObject) subentry.getValue();
                    String stacktrace = array.getString("stacktrace");
                    String slicing_count = array.getString("slicing_count");
                    String predicateswitching = array.getString("predicateswitching");
                    String slicing_intersection = array.getString("slicing_intersection");
                    String metallaxis = array.getString("metallaxis");
                    String ochiai = array.getString("ochiai");
                    String muse = array.getString("muse");
                    String slicing = array.getString("slicing");
                    String dstar = array.getString("dstar");
                    String faulty = array.getString("faulty");
                    fw.write(className+"#"+line+"\t"+
                            stacktrace+"\t"+slicing_count+"\t"+
                            predicateswitching+"\t"+slicing_intersection+"\t"+
                            metallaxis+"\t"+ochiai+"\t"+
                            muse+"\t"+slicing+"\t"+
                            dstar+"\t"+faulty+"\n");
                }
                fw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
