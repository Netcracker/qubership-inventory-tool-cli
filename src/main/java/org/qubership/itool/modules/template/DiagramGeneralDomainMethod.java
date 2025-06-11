package org.qubership.itool.modules.template;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.qubership.itool.modules.diagram.DiagramBuilder;
import org.qubership.itool.modules.diagram.DiagramService;

import java.util.List;

public class DiagramGeneralDomainMethod implements TemplateMethodModelEx {

    private DiagramService diagramService;

    public DiagramGeneralDomainMethod(DiagramService diagramService) {
        this.diagramService = diagramService;
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        try {
            String defaultDomainId = null;
            String department = null;
            if (arguments.get(0).getClass() == SimpleScalar.class) {
                department = ((SimpleScalar) arguments.get(0)).getAsString();
                if (arguments.size() >= 2) {
                    defaultDomainId = ((SimpleScalar) arguments.get(1)).getAsString();
                }
            } else {
                department = (String) arguments.get(0);
                if (arguments.size() >= 2) {
                    defaultDomainId = (String) arguments.get(1);
                }
            }
            DiagramBuilder diagramBuilder = new DiagramBuilder()
                    .department(department)
                    .defaultDomainLevelEntity(defaultDomainId);

            String plantUml = this.diagramService.generate("general-domains", diagramBuilder.build());
            return plantUml == null ? "" : plantUml;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

}