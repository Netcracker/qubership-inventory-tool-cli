package org.qubership.itool.modules.diagram.providers;

import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.diagram.Diagram;
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.graph.BasicGraph;

import java.util.Properties;

public interface DiagramProvider {

    String SKINPARAM_BACKGROUND_COLOR_DEFAULT_DOMAIN = "diagram.skinparam.backgroundColor.default.domain";
    String SKINPARAM_BACKGROUND_COLOR_DEFAULT_COMPONENT = "diagram.skinparam.backgroundColor.default.component";

    String SKINPARAM_BACKGROUND_COLOR = "diagram.skinparam.backgroundColor";
    String SKINPARAM_BACKGROUND_COLOR_C = "diagram.skinparam.backgroundColor.c.";

    String SKINPARAM_BACKGROUND_COLOR_DATABASE = "diagram.skinparam.backgroundColor.database";

    String SKINPARAM_BACKGROUND_COLOR_QUEUE = "diagram.skinparam.backgroundColor.queue";
    String SKINPARAM_BACKGROUND_COLOR_INDEXATION = "diagram.skinparam.backgroundColor.indexation";
    String SKINPARAM_BACKGROUND_COLOR_CACHING = "diagram.skinparam.backgroundColor.caching";

    void setProperties(Properties properties);

    Properties getProperties();

    String getType();

    default String generate(DiagramService diagramService, BasicGraph graph, Diagram diagram) {
        return null;
    }

    default String wrap(String value) {
        return value.replaceAll("-|,|\\.|\\s", "_");
    }

    default String header(BasicGraph graph) {
        StringBuilder result = new StringBuilder();
        result.append("@startuml\n\nscale 0.8\nleft to right direction\nskinparam defaultTextAlignment center\n\n");
        result.append(skinparam(graph));
        return result.toString();
    }

    default String skinparam(BasicGraph graph) {
        StringBuilder result = new StringBuilder();
        result.append("skinparam database {\n  backgroundColor ");
        result.append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_DATABASE, "GreenYellow"));
        result.append("\n}\n");
        result.append("skinparam queue {\n  backgroundColor ");
        result.append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_QUEUE, "GreenYellow"));
        result.append("\n}\n");
        result.append(skinparamDomains(graph));
        result.append("}\n");
        return result.toString();
    }

    default String skinparamDomains(BasicGraph graph) {
        StringBuilder result = new StringBuilder();
        result.append("skinparam rectangle {\n")
            .append("  backgroundColor<<database>> ")
            .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_DATABASE, "MistyRose"))
            .append("\n")
            .append("  backgroundColor<<indexation>> ")
            .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_INDEXATION, "MistyRose"))
            .append("\n")
            .append("  backgroundColor<<caching>> ")
            .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_CACHING, "MistyRose"))
            .append("\n");
        for (JsonObject d : graph.traversal().V().hasType("domain").toList()) {
            String backgroundColor = getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_C + d.getString("id"), "Gainsboro");
            result.append("  backgroundColor<<" + d.getString("id") + ">> " + backgroundColor + "\n");
        }
        return result.toString();
    }

    default String footer(BasicGraph graph) {
        return "\n@enduml\n";
    }

    default String relation(String sourceId, String destinationId, String edgeType, String edgeName, boolean overrideType) {
        StringBuilder tmp = new StringBuilder();
        tmp.append(wrap(sourceId));
        if ("GQL".equals(destinationId)) {
            tmp.append(" #-[#blue]- ");
        } else if ("GQL".equals(sourceId)) {
            return null;
        } else if (sourceId.equals(destinationId)) {
            return null;
        } else if("optional".equals(edgeType)) {
            if (overrideType) {
                tmp.append(" --> ");
            } else {
                tmp.append(" .[#gray].> ");
            }
        } else {
            tmp.append(" --> ");
        }
        tmp.append(wrap(destinationId));

        if (edgeName != null) {
            tmp.append(" : ").append(edgeName.replaceAll("$\\{spring.application.cloud_namespace}", ""));
        }
        return tmp.toString();
    }

    default String relation(String sourceId, String destinationId, String edgeType, boolean overrideType) {
        return relation(sourceId, destinationId, edgeType, null, overrideType);
    }

    default String component(String mainDomain, JsonObject component) {
        String componentType = component.getString("type");
        String type = "rectangle";
        if ("caching".equals(componentType) || "database".equals(componentType) || "indexation".equals(componentType)) {
            type = "database";
        }
        if ("mq".equals(componentType)) {
            type = "queue";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(type).append(" \"").append(component.getString("name")).append("\"");

        String domain = null;
        if (component.getJsonObject("details") != null) {
            domain = component.getJsonObject("details").getString("domain");
        }
        if (domain == null && "domain".equals(component.getString("type"))) {
            domain = component.getString("id");
        }
        if ((mainDomain == null && domain != null) || (domain != null && mainDomain != null && !mainDomain.equals(domain))) {
            builder.append(" <<").append(domain.toUpperCase()).append(">> ");
        } else if ("caching".equals(componentType) || "database".equals(componentType) || "indexation".equals(componentType)) {
                builder.append(" <<").append(componentType).append(">>");
        }
        builder.append(" as ").append(wrap(component.getString("id")));

        return builder.toString();
    }

}
