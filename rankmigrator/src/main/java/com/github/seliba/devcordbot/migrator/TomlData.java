/*
 * Copyright 2020 Daniel Scherf & Michael Rittmeister
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.seliba.devcordbot.migrator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages a TOML file and offers options to save and read data from the TOML file
 * format.
 *
 * @author Seliba
 * @version 1.0
 */
public class TomlData {

    private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);
    private File file;
    private Map<String, Object> entrys = new HashMap<>();

    /**
     * @param fileName The name of the file where the values should be stored
     */
    public TomlData(String fileName) {
        file = new File(fileName + ".toml");
        reload();
    }

    /**
     * Reloads all data that is stored in the TOML file
     */
    public void reload() {
        try {
            if (!file.exists()) {
                file.createNewFile();
                LOGGER.info("Neuer File erstellt!");
            }

            Scanner scanner = new Scanner(file);
            entrys.clear();
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                String[] arguments = nextLine.split(" ");
                if (arguments[1].equals("=")) {
                    Object value;
                    if (Long.valueOf(arguments[2]) != null) {
                        value = Long.valueOf(arguments[2]);
                    } else {
                        value = arguments[2];
                    }
                    entrys.put(arguments[0], value);
                }
            }
            scanner.close();
        } catch (IOException exception) {
            LOGGER.error("An error occurred while parsing file", exception);
        }
    }

    /**
     * Set the path to the given value. The method creates new paths if they don't already exist
     *
     * @param path The path where the value should be stored
     * @param value The value that should be stored
     */
    public synchronized void set(String path, Object value) {
        entrys.remove(path);
        entrys.put(path, value);
        this.save();
    }

    /**
     * Gets the value of a path, in this case of an Long
     *
     * @param path The path of the value in the TOML file
     * @return The value that is defined in the TOML file
     */
    public Long getLong(String path) {
        return (Long) entrys.get(path);
    }

    /**
     * Gets the value of a path, in this case of an String
     *
     * @param path The path of the value in the TOML file
     * @return The value that is defined in the TOML file
     */
    public String getString(String path) {
        return (String) entrys.get(path);
    }

    /**
     * @return All entries that are saved in the TOML file
     */
    public Map<String, Object> getEntries() {
        return entrys;
    }

    public void delete() {
        file.delete();
    }

    /**
     * Saves the current data to the TOML file
     */
    public void save() {
        List<String> lines = new ArrayList<>();
        entrys.forEach((string, object) -> lines.add(string + " = " + (object)));
        Path filePath = Paths.get(file.getPath());
        try {
            Files.write(filePath, lines, Charset.forName("UTF-8"));
        } catch (IOException exception) {
            LOGGER.atError().setCause(exception).log("Error while saving file {}!", file.getName());
        }
    }
}