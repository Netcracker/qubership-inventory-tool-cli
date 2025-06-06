/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.itool.modules.template;

import io.vertx.core.json.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfluencePage {
    private JsonObject element; // can be Domain, Component, Report or something else
    private String elementType;
    private String title;
    private String parentTitle;
    private String type;
    private String space;
    private String template;
    private String directoryPath;
    private String fileName;
    private Map<String, Object> dataModel = new LinkedHashMap<>();

    public ConfluencePage() {
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Map<String, Object> getDataModel() {
        if (this.elementType != null && this.element != null) {
            this.dataModel.put(this.elementType, this.element);
        }
        return dataModel;
    }

    public void setDataModel(Map<String, Object> dataModel) {
        this.dataModel = dataModel;
    }

    public void addDataModel(String key, Object data) {
        this.dataModel.put(key, data);
    }

    public String getElementType() {
        return elementType;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }

    public JsonObject getElement() {
        return element;
    }

    public void setElement(JsonObject element) {
        this.element = element;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    @Override
    public String toString() {
        return "ConfluencePage{" +
                "element=" + element +
                "\n, elementType='" + elementType + '\'' +
                "\n, title='" + title + '\'' +
                "\n, parentTitle='" + parentTitle + '\'' +
                "\n, space='" + space + '\'' +
                "\n, template='" + template + '\'' +
                "\n, directoryPath='" + directoryPath + '\'' +
                "\n, fileName='" + fileName + '\'' +
                "\n, dataModel=" + dataModel +
                '}';
    }

    public String getParentTitle() {
        return parentTitle;
    }

    public void setParentTitle(String parentTitle) {
        this.parentTitle = parentTitle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
