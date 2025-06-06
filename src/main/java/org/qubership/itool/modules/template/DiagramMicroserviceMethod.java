package org.qubership.itool.modules.template;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.qubership.itool.modules.diagram.DiagramBuilder;
import org.qubership.itool.modules.diagram.DiagramService;

import java.util.List;

public class DiagramMicroserviceMethod implements TemplateMethodModelEx {

    private DiagramService diagramService;

    public DiagramMicroserviceMethod(DiagramService diagramService) {
        this.diagramService = diagramService;
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        try {
            String department = null;
            String defaultDomainId = null;
            String defaultComponent = null;
            if (arguments.get(0).getClass() == SimpleScalar.class) {
                department = ((SimpleScalar) arguments.get(0)).getAsString();
                defaultDomainId = ((SimpleScalar) arguments.get(1)).getAsString();
                defaultComponent = ((SimpleScalar) arguments.get(2)).getAsString();
            } else {
                department = (String) arguments.get(0);
                defaultDomainId = (String) arguments.get(1);
                defaultComponent = (String) arguments.get(2);
            }

            DiagramBuilder diagramBuilder = new DiagramBuilder()
                    .department(department)
                    .defaultDomainLevelEntity(defaultDomainId)
                    .defaultComponent(defaultComponent);
            if (!"Platform".equals(department)) {
                diagramBuilder.excludeDomains("Platform");
            }

            String plantUml = this.diagramService.generate("microservice", diagramBuilder.build());
            return plantUml == null ? "" : plantUml;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

}