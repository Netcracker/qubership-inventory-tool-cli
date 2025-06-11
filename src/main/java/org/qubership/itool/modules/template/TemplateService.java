package org.qubership.itool.modules.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.qubership.itool.modules.diagram.DiagramService;

import java.io.IOException;

public interface TemplateService {
    String BASE_TEMPLATES_PATH = "config/templates2";

    Configuration configure() throws IOException;
    Template getTemplate(String templateName);

    String processTemplate(ConfluencePage confluencePage) throws TemplateException, IOException;

    DiagramService getDiagramService();
}
