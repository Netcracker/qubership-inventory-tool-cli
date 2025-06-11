package org.qubership.itool.modules.diagram.providers;

import org.qubership.itool.modules.diagram.Diagram;
import org.qubership.itool.modules.graph.BasicGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractQueueDiagramProvider implements DiagramProvider {

    private Properties properties;

    static final String QUEUE_STRING = "queue \"";
    static final String RECTANGLE_STRING = "rectangle \"";


    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public String header(BasicGraph graph) {
        StringBuilder builder = new StringBuilder();
        builder.append("@startuml\n\n");
        builder.append("scale 0.8\n");
        builder.append("left to right direction\n");
        builder.append("skinparam defaultTextAlignment center\n\n");
        builder.append("skinparam queueBackgroundColor ")
                .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_QUEUE, "GreenYellow"))
                .append("\n");
        builder.append("skinparam rectangleBackgroundColor ")
                .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_DEFAULT_COMPONENT, "Yellow"))
                .append("\n");
        return builder.toString();
    }

    @Override
    public String footer(BasicGraph graph) {
        return "\n@enduml\n";
    }

    protected String generateGraphSchema(Diagram diagram, BasicGraph graph) {
        StringBuilder relationBuilder = new StringBuilder();
        StringBuilder builder = new StringBuilder();
        builder.append(header(graph));
        List<String> producersNames = new ArrayList<>();
        List<String> consumersNames = new ArrayList<>();
        List<String> queueNames = new ArrayList<>();
        for (Map<String, Object> singleProducer : diagram.getProducerList()) {
            String queueName = singleProducer.get("queueName").toString();
            if (!queueNames.contains(queueName)) {
                queueNames.add(queueName);
            }
            String producerName = singleProducer.get("producerName").toString();
            if (!producersNames.contains(producerName)) {
                producersNames.add(producerName);
            }
            if (queueHasDualLinkToComponent(diagram.getConsumerList(), queueName, producerName, "consumerName")) {
                relationBuilder.append(charReplace(queueName))
                        .append(" <-[#deepSkyBlue]-> ")
                        .append(charReplace(producerName)).append(": ").append(queueName).append("\n");
            } else {
                relationBuilder.append(charReplace(producerName))
                        .append(" -[#green]-> ")
                        .append(charReplace(queueName)).append(": <<producer>> ").append(queueName).append("\n");
            }
        }
        for (Map<String, Object> singleConsumer : diagram.getConsumerList()) {
            String queueName = singleConsumer.get("queueName").toString();
            String consumerName = singleConsumer.get("consumerName").toString();
            if (!queueHasDualLinkToComponent(diagram.getProducerList(), queueName, consumerName, "producerName")) {
                relationBuilder.append(charReplace(queueName))
                        .append(" -[#blue]-> ")
                        .append(charReplace(consumerName)).append(": <<consumer>> ").append(queueName).append("\n");
                if (!consumersNames.contains(consumerName)) {
                    consumersNames.add(consumerName);
                }
                if (!queueNames.contains(queueName)) {
                    queueNames.add(queueName);
                }
            }
        }
        builder.append(mappingBuilder(queueNames, QUEUE_STRING));
        builder.append(mappingBuilder(producersNames, RECTANGLE_STRING));
        builder.append(mappingBuilder(consumersNames, RECTANGLE_STRING));
        builder.append(relationBuilder);
        builder.append(footer(graph));
        return builder.toString();
    }

    private String mappingBuilder(List<String> nameList, String type) {
        StringBuilder builder = new StringBuilder();
        if (type.equals("queue \"")) {
            for (String data : nameList) {
                builder.append(type).append(charReplace(data))
                        .append("\" as ").append(charReplace(data)).append("\n");
            }
        } else {
            for (String data : nameList) {
                builder.append(type).append(data)
                        .append("\" as ").append(charReplace(data)).append("\n");
            }
        }
        return builder.toString();
    }

    protected String charReplace(String str) {
        return str.replaceAll("/|\\s|&|:|\\$\\{.+\\:(.+)\\}|\\(\\)|\\$\\{.+\\}", "$1")
                .replaceAll("\\_+(.+)", "$1").replaceAll("(\\w)\\_*", "$1")
                .replaceAll("\\s", "").replaceAll("\\(.+\\)", "")
                .replaceAll("\\-", "_").replaceAll("\\.", "_")
                .replaceAll("\\_(.+)", "$1").replaceAll("(.+)\\_", "$1").replaceAll("\\#\\{.+\\}(.+)", "$1");
    }

    protected Boolean queueHasDualLinkToComponent(List<Map<String, Object>> componentsList, String queueName, String checkedComponentName, String componentNameKey) {
        for (Map<String, Object> nextComponent : componentsList) {
            String nextQueueName = nextComponent.get("queueName").toString();
            String nextComponentName = nextComponent.get(componentNameKey).toString();
            if (queueName.equals(nextQueueName) && checkedComponentName.equals(nextComponentName)) {
                return true;
            }
        }
        return false;
    }
}
