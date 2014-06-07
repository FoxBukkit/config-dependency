/**
 * This file is part of ConfigDependency.
 *
 * ConfigDependency is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ConfigDependency is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ConfigDependency.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.dependencies.config;

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
                    lpos = line.indexOf('=');
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
