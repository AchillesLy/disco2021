import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tools.JsonUtils;
import tools.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

public class Main {
    public static final Logger logger =  LogManager.getLogger("Decomposer");
    private static String discoDB;
    public static void main(String[] args) throws IOException {
        /*
        Wiring，把输入的swagger扔到一个叫./jsons的文件夹里。
           File inputPath = new File("./jsons/");
        Wiring, Drop the input swagger in a folder named ./jsons
        */
        File inputPath = new File("./input/");

        
        // 所有模式的路径 --> "./schemaOrgTree.jsonld" "2"
        // path all schema --> "./schemaOrgTree.jsonld" "2"
        File contextFile = new File("./schemaOrgTree.jsonld");
        String resultsPath = "./results/";

        //磁盘db的路径 --> "/XXX/discoDb"。
        //path to disk db --> "/XXX/discoDb".
        discoDB = "~/Documents/DISCO/enwiki-20130403-sim-lemma-mwl-lc";
        String discoDbPath = discoDB;

        // 认为相似性足够强的阈值 --> 例如，"2"
        //threshold to consider that a similarity is sufficiently strong --> e.g. "2",recommended threashold "1.5"
        Float threshold = Float.parseFloat("1.5");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootContextNode = objectMapper.readTree(contextFile);
        long timestamp = System.currentTimeMillis();

        File[] listInput = inputPath.listFiles();
        Utils.initializeDictionaries(discoDbPath);

        for (int i = 0; i < listInput.length ; i++) {
            FileWriter oStream = new FileWriter(resultsPath + timestamp + "_" + listInput[i].getName());
            BufferedWriter out = new BufferedWriter(oStream);
            JsonNode inputNode = objectMapper.readTree(listInput[i]);
            try {
                JsonUtils.AnalyzeJson(inputNode, rootContextNode, "operations", out);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        BuildResult(resultsPath,timestamp, rootContextNode, threshold);

        float seconds = (System.currentTimeMillis()-timestamp)/1000;
        logger.info("The Process has taken: " + seconds + " seconds");
    }

    private static void BuildResult(String resultPath, long timestamp, JsonNode rootContextNode, Float threshold) {
        File resFolder = new File(resultPath);
        File[] resInput = resFolder.listFiles();

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(resultPath+"/"+timestamp+"_result.txt"));
            for (int i = 0; i < resInput.length; i++) {
                File resActual = resInput[i];
                if (resActual.getName().contains(String.valueOf(timestamp))){
                    BufferedReader br;
                    try {
                        br = new BufferedReader(new FileReader(resActual));
                        String line;

                        Hashtable<String,Integer> hash =new Hashtable<>();

                        while ((line = br.readLine())!= null){
                            String[] lineaActual = line.split(";");

                            if (Float.parseFloat(lineaActual[2]) <= threshold){
                                int level = 0;
                                JsonNode contextNode = JsonUtils.LocateInJsonTree(rootContextNode, lineaActual[1], "children", level);
                                if (!(contextNode.isMissingNode())){
                                    logger.info(contextNode.get("name"));
                                    JsonNode parentNode = JsonUtils.LocateInJsonTree(rootContextNode, contextNode.get("name").asText(),"children",level);
                                    if (hash.containsKey(parentNode.get("name").asText())){
                                        Integer newVal = hash.get(parentNode.get("name").asText()) + 1;
                                        hash.remove(parentNode.get("name").asText());
                                        hash.put(parentNode.get("name").asText(),newVal);
                                    }
                                    else hash.put(parentNode.get("name").asText(),1);
                                    logger.info(parentNode.get("name"));
                                    logger.info(hash.toString());
                                }
                            }
                        }
                        final Set<Map.Entry<String, Integer>> entries = hash.entrySet();
                        Iterator iter = entries.iterator();
                        while (iter.hasNext()){
                            Map.Entry<String,Integer> element = (Map.Entry<String, Integer>) iter.next();
                            String aux = resActual.getName().split("_")[1];
                            bw.write(aux.substring(0,aux.lastIndexOf("."))+";");
                            bw.write(element.getKey()+";"+element.getValue());
                            bw.write("\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
