package rega.genotype.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.ui.util.FileUtil;
import rega.genotype.ui.util.GsonUtil;

/**
 * Read json config
 * Contains general system (env) configuration and a list of specific tool 
 * configurations for this server. 
 * 
 * @author michael
 *
 */
public class Config {
	private GeneralConfig generalConfig;
	private List<ToolConfig> tools = new ArrayList<Config.ToolConfig>();

	public static Config parseJson(String json) {
		return GsonUtil.parseJson(json, Config.class);
	}

	public String toJson() {
		return GsonUtil.toJson(this);
	}

	public GeneralConfig getGeneralConfig() {
		return generalConfig;
	}

	public void setGeneralConfig(GeneralConfig generalConfig) {
		this.generalConfig = generalConfig;
	}

	public ToolConfig getToolConfigByToolId(String toolId) {
		ToolConfig toolConfig = null;
		// find organism config
		for (ToolConfig c: getTools())
			if (c.getToolId() != null && c.getToolId().equals(toolId))
				toolConfig = c;

		return toolConfig;
	}

	/**
	 * @param url url path component that defines the tool 
	 * @return
	 */
	public ToolConfig getToolConfigByUrlPath(String url) {
		ToolConfig toolConfig = null;
		// find organism config
		for (ToolConfig c: getTools())
			if (c.getPath().equals(url))
				toolConfig = c;

		return toolConfig;
	}

	public List<ToolConfig> getTools() {
		return tools;
	}

	public void setTools(List<ToolConfig> tools) {
		this.tools = tools;
	}

	// classes

	public static class GeneralConfig {
		private String paupCmd;
		private String clustalWCmd;
		private String blastPath;
		private String treePuzzleCmd;
		private String treeGraphCmd;
		private String epsToPdfCmd;
		private String imageMagickConvertCmd;
		private int maxAllowedSeqs;
		private String inkscapeCmd;
		private String adminPassword;
		private String repositoryId;
		
		public String getPaupCmd() {
			return paupCmd;
		}
		public void setPaupCmd(String paupCmd) {
			this.paupCmd = paupCmd;
		}
		public String getClustalWCmd() {
			return clustalWCmd;
		}
		public void setClustalWCmd(String clustalWCmd) {
			this.clustalWCmd = clustalWCmd;
		}
		public String getTreePuzzleCmd() {
			return treePuzzleCmd;
		}
		public void setTreePuzzleCmd(String treePuzzleCmd) {
			this.treePuzzleCmd = treePuzzleCmd;
		}
		public String getTreeGraphCmd() {
			return treeGraphCmd;
		}
		public void setTreeGraphCmd(String treeGraphCmd) {
			this.treeGraphCmd = treeGraphCmd;
		}
		public String getEpsToPdfCmd() {
			return epsToPdfCmd;
		}
		public void setEpsToPdfCmd(String epsToPdfCmd) {
			this.epsToPdfCmd = epsToPdfCmd;
		}
		public String getImageMagickConvertCmd() {
			return imageMagickConvertCmd;
		}
		public void setImageMagickConvertCmd(String imageMagickConvertCmd) {
			this.imageMagickConvertCmd = imageMagickConvertCmd;
		}
		public int getMaxAllowedSeqs() {
			return maxAllowedSeqs;
		}
		public void setMaxAllowedSeqs(int maxAllowedSeqs) {
			this.maxAllowedSeqs = maxAllowedSeqs;
		}
		public String getInkscapeCmd() {
			return inkscapeCmd;
		}
		public void setInkscapeCmd(String inkscapeCmd) {
			this.inkscapeCmd = inkscapeCmd;
		}
		public String getBlastPath() {
			return blastPath;
		}
		public void setBlastPath(String blastPath) {
			this.blastPath = blastPath;
		}
		public String getAdminPassword() {
			return adminPassword;
		}
		public void setAdminPassword(String adminPassword) {
			this.adminPassword = adminPassword;
		}
		public String getRepositoryId() {
			return repositoryId;
		}
		public void setRepositoryId(String repositoryId) {
			this.repositoryId = repositoryId;
		}
	}

	public static class ToolConfig {
		private String path; // url path component that defines the tool (default toolId)
		private String configuration; // xmlPath - the directory that contains all the tool files.
		private String jobDir; // will contain all the work dirs of the tool. (Generated analysis data)
		private boolean autoUpdate;
		private boolean webService;
		private boolean ui;
		// ToolMenifest read manifests from configuration dir.
		// TODO: ui will have to update manifest if it was changed.
		transient private ToolMenifest manifest = null;

		public ToolMenifest getToolMenifest() {
			File f = new File(configuration + File.separator + "manifest.json");
			if (f.exists() && manifest == null) {
				String json = FileUtil.readFile(f);
				manifest = ToolMenifest.parseJson(json);
			}
			return manifest;
		}
		public String getToolId() {
			return getToolMenifest() == null ? null : getToolMenifest().getToolId();
		}
		public String getPath() {
			if (path != null)
				return path;
			else
				return getToolMenifest().getToolId();
		}
		public void setPath(String path) {
			this.path = path;
		}
		public String getConfiguration() {
			return configuration;
		}
		public void setConfiguration(String configuration) {
			this.configuration = configuration;
		}
		public boolean isAutoUpdate() {
			return autoUpdate;
		}
		public void setAutoUpdate(boolean autoUpdate) {
			this.autoUpdate = autoUpdate;
		}
		public String getJobDir() {
			return jobDir;
		}
		public void setJobDir(String jobDir) {
			this.jobDir = jobDir;
		}
		public boolean isWebService() {
			return webService;
		}
		public void setWebService(boolean webService) {
			this.webService = webService;
		}
		public boolean isUi() {
			return ui;
		}
		public void setUi(boolean ui) {
			this.ui = ui;
		}
	}
}