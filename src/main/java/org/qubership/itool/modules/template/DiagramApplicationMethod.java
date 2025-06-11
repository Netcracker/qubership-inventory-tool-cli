package org.qubership.itool.modules.template;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.qubership.itool.modules.diagram.DiagramBuilder;
import org.qubership.itool.modules.diagram.DiagramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DiagramApplicationMethod implements TemplateMethodModelEx {

    private static final Logger LOG = LoggerFactory.getLogger(DiagramApplicationMethod.class);

    private DiagramService diagramService;

    public DiagramApplicationMethod(DiagramService diagramService) {
        this.diagramService = diagramService;
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        try {
            String plantUml = null;
            String defaultApplicationId = null;
            if (arguments.size() == 2) {
                defaultApplicationId = (String) arguments.get(0);
                DiagramBuilder diagramBuilder = new DiagramBuilder()
                        .defaultDomainLevelEntity(defaultApplicationId);

                plantUml = this.diagramService.generate(arguments.get(1).toString(), diagramBuilder.build());
            }
            return plantUml == null ? "" : plantUml;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
