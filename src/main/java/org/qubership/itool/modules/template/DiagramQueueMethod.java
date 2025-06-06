package org.qubership.itool.modules.template;

import freemarker.template.DefaultListAdapter;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.qubership.itool.modules.diagram.DiagramBuilder;
import org.qubership.itool.modules.diagram.DiagramService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiagramQueueMethod implements TemplateMethodModelEx {

    private DiagramService diagramService;

    public DiagramQueueMethod(DiagramService diagramService) {
        this.diagramService = diagramService;
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        try {
            List<Map<String, Object>> producers = new ArrayList<>();
            List<Map<String, Object>> consumers = new ArrayList<>();
            if (arguments.get(0).getClass() == DefaultListAdapter.class) {
                producers.addAll((List<Map<String, Object>>)((DefaultListAdapter) arguments.get(0)).getWrappedObject());
                consumers.addAll((List<Map<String, Object>>)((DefaultListAdapter) arguments.get(1)).getWrappedObject());
            } else {
                producers = (List<Map<String, Object>>) arguments.get(0);
                consumers = (List<Map<String, Object>>) arguments.get(1);
            }

            DiagramBuilder diagramBuilder = new DiagramBuilder()
                    .producers(producers).consumers(consumers);

            String plantUml = this.diagramService.generate("rabbitMQ", diagramBuilder.build());
            return plantUml == null ? "" : plantUml;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
