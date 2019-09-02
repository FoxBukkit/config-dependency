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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Configuration extends ConcurrentHashMap<String, String> {
    private final File dataFolder;
    private final String dataFile;
    private final Object lockObject;
    private final Set<OnChangeHook> hooks;
    private boolean isLoading = true;

    public Configuration(File dataFolder) {
        this(dataFolder, "config.txt");
    }

    public Configuration(File dataFolder, String fileName) {
        this.dataFolder = dataFolder;
		dataFolder.mkdirs();
        this.dataFile = fileName;
        this.hooks = new HashSet<>();
        this.lockObject = new Object();
        load();
    }

    public interface OnChangeHook {
        /**
         * Entry change delegate functional interface
         * @param key Key of changed entry
         * @param value Value of changed entry (null if removed)
         */
        void onEntryChanged(String key, String value);
    }
    public void addOnChangeHook(OnChangeHook hook)  {
        synchronized (lockObject) {
            this.hooks.add(hook);
        }
    }

    private void triggerChange(String key, String value) {
        synchronized (lockObject) {
            for (OnChangeHook hook : hooks) {
                hook.onEntryChanged(key, value);
            }
        }

        this.save();
    }

    public FileReader makeReader(String file) throws IOException {
        return new FileReader(new File(dataFolder, file), StandardCharsets.UTF_8);
    }

    public FileWriter makeWriter(String file) throws IOException {
        return new FileWriter(new File(dataFolder, file), StandardCharsets.UTF_8);
    }

    public void load() {
        synchronized (lockObject) {
            this.isLoading = true;
            this.clear();
            try {
                BufferedReader stream = new BufferedReader(makeReader(dataFile));
                String line;
                int lpos;
                while ((line = stream.readLine()) != null) {
                    lpos = line.indexOf('=');
                    if (lpos > 0) {
                        this.put(line.substring(0, lpos), line.substring(lpos + 1));
                    }
                }
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.isLoading = false;
        }
    }

    public void save() {
        synchronized (lockObject) {
            if (isLoading) {
                return;
            }

            try {
                PrintWriter stream = new PrintWriter(makeWriter(dataFile));
                for (Map.Entry<String, String> configEntry : this.entrySet()) {
                    stream.println(configEntry.getKey() + "=" + configEntry.getValue());
                }
                stream.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public String put(String key, String value) {
        String res = super.put(key, value);
        this.triggerChange(key, value);
        return res;
    }

    @Override
    public String remove(Object key) {
        String res = super.remove(key);
        this.triggerChange(key.toString(), null);
        return res;
    }

    @Override
    public void clear() {
        Collection<String> oldKeys = new ArrayList<>(this.keySet());
        super.clear();
        for (String key : oldKeys) {
            this.triggerChange(key, null);
        }
    }

    public String getValue(String key, String def) {
        synchronized (lockObject) {
            if (this.containsKey(key)) {
                return this.get(key);
            }
            this.put(key, def);
        }
        save();
        return def;
    }
}
