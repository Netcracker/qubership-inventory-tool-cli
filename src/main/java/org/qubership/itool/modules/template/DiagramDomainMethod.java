package org.qubership.itool.modules.template;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.qubership.itool.modules.diagram.DiagramBuilder;
import org.qubership.itool.modules.diagram.DiagramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DiagramDomainMethod implements TemplateMethodModelEx {

    private static final Logger LOG = LoggerFactory.getLogger(DiagramDomainMethod.class);

    private DiagramService diagramService;

    public DiagramDomainMethod(DiagramService diagramService) {
        this.diagramService = diagramService;
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        try {
            String defaultDomainId = null;
            String department = null;
            if (arguments.get(0).getClass() == SimpleScalar.class) {
                department = ((SimpleScalar) arguments.get(0)).getAsString();
                defaultDomainId = ((SimpleScalar) arguments.get(1)).getAsString();
            } else {
                department = (String) arguments.get(0);
                defaultDomainId = (String) arguments.get(1);
            }

            DiagramBuilder diagramBuilder = new DiagramBuilder()
                    .department(department)
                    .defaultDomainLevelEntity(defaultDomainId);

            String plantUml = this.diagramService.generate("domain", diagramBuilder.build());
            return plantUml == null ? "" : plantUml;

        } catch (Exception ex) {
            LOG.error("Exception in freemarker call", ex);
            throw ex;
        }
    }

}