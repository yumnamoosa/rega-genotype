/*
 * Copyright (C) 2013 Emweb
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.generic;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.jdom.JDOMException;

import rega.genotype.ui.data.Config.ToolConfig;
import rega.genotype.ui.forms.DocumentationForm;
import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import eu.webtoolkit.jwt.Configuration.ErrorReporting;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WXmlLocalizedStrings;

/**
 * Enterovirus implementation of the genotype application.
 */
@SuppressWarnings("serial")
public class GenericMain extends GenotypeMain {
	public GenericMain() {
		super();
		getConfiguration().setInternalDeploymentSize(1);
		
		getConfiguration().setInlineCss(false);
		getConfiguration().setProgressiveBootstrap(true);
		getConfiguration().setErrorReporting(ErrorReporting.NoErrors);
	}
	
	@Override
	public WApplication createApplication(WEnvironment env) {		
		String[] deploymentPath = env.getDeploymentPath().split("/");
		String urlComponent = deploymentPath[deploymentPath.length - 1];
		
		GenotypeApplication app = new GenotypeApplication(env, 
				this.getServletContext(), settings, urlComponent);

		
		ToolConfig toolConfig = settings.getConfig().getToolConfig(urlComponent);

		if (toolConfig == null) {
			app.getRoot().addWidget(new WText("Typing tool for organism " + urlComponent + " was not found."));
			return app;
		}

		GenericDefinition definition;
		try {
			definition = new GenericDefinition(toolConfig.getConfiguration(), toolConfig.getJobDir());
		} catch (JDOMException e) {
			e.printStackTrace();
			showErrorMsg(app);
			return app;
		} catch (IOException e) {
			e.printStackTrace();
			showErrorMsg(app);
			return app;
		}

		WXmlLocalizedStrings resources = new WXmlLocalizedStrings();
		resources.use("/rega/genotype/ui/i18n/resources/common_resources");
		resources.use(definition.getXmlFolder() + "resources");
		app.setLocalizedStrings(resources);
		
		app.setTitle(WString.tr("tool.title"));
//		app.useStyleSheet(new WLink("../style/genotype-rivm.css"));
//		app.useStyleSheet(new WLink("../style/genotype-rivm-ie.css"), "IE lte 7");
		
		app.useStyleSheet(new WLink("../../style/genotype-rivm.css"));
		app.useStyleSheet(new WLink("../../style/genotype-rivm-ie.css"), "IE lte 7");

		GenotypeWindow window = new GenotypeWindow(definition);

		for (GenericDefinition.MenuItem item : definition.getMenuItems())
			window.addForm(item.label, item.path, new DocumentationForm(window, WString.tr(item.messageId)));
		
		window.init();

		app.getRoot().addWidget(window);
		
		return app;
	}
	
	private void showErrorMsg(GenotypeApplication app) {
		app.getRoot().addWidget(new WText("Typing tool for given url does not exist."));
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		//this.organism = config.getInitParameter("Organism");
		super.init(config);
	}
}
