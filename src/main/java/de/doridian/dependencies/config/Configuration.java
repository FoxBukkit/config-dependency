package de.doridian.dependencies.config;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Configuration {
    private final HashMap<String,String> configValues;
    private final File dataFolder;
    private final String dataFile;

    public Configuration(File dataFolder) {
        this(dataFolder, "config.txt");
    }

    public Configuration(File dataFolder, String fileName) {
        this.configValues = new HashMap<>();
        this.dataFolder = dataFolder;
        this.dataFile = fileName;
        load();
    }

    public FileReader makeReader(String file) throws FileNotFoundException {
        return new FileReader(new File(dataFolder, file));
    }

    public FileWriter makeWriter(String file) throws IOException {
        return new FileWriter(new File(dataFolder, file));
    }

    public void load() {
        synchronized (configValues) {
            configValues.clear();
            try {
                BufferedReader stream = new BufferedReader(makeReader(dataFile));
                String line;
                int lpos;
                while ((line = stream.readLine()) != null) {
                    lpos = line.lastIndexOf('=');
                    if (lpos > 0)
                        configValues.put(line.substring(0, lpos), line.substring(lpos + 1));
                }
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        synchronized (configValues) {
            try {
                PrintWriter stream = new PrintWriter(makeWriter(dataFile));
                for (Map.Entry<String, String> configEntry : configValues.entrySet()) {
                    stream.println(configEntry.getKey() + "=" + configEntry.getValue());
                }
                stream.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public String getValue(String key, String def) {
        synchronized (configValues) {
            if (configValues.containsKey(key)) {
                return configValues.get(key);
            }
            configValues.put(key, def);
        }
        save();
        return def;
    }
}
