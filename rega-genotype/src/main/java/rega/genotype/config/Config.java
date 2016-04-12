package rega.genotype.config;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.utils.FileUtil;
import rega.genotype.utils.GsonUtil;
import rega.genotype.utils.Settings;

/**
 * Read json config
 * Contains general system (env) configuration and a list of specific tool 
 * configurations for this server. 
 * 
 * @author michael
 *
 */
public class Config {
	public static final String CONFIG_FILE_NAME = "config.json";

	private GeneralConfig generalConfig = new GeneralConfig();
	private List<ToolConfig> tools = new ArrayList<Config.ToolConfig>(); //TODO: use set 

	public Config(){}
	
	public static Config parseJson(String json) {
		return GsonUtil.parseJson(json, Config.class);
	}

	public String toJson() {
		return GsonUtil.toJson(this);
	}

	public void save() throws IOException {
		save(Settings.getInstance().getBaseDir() + File.separator);
	}

	private void save(String externalDir) throws IOException {
		// Note: no need to synchronize: The file is read only 1 time when setting
		// is constructed. Setting config state is constantly updated by the UI.

		if (getGeneralConfig().getPublisherPassword() == null 
				|| getGeneralConfig().getPublisherPassword().isEmpty()) 
			getGeneralConfig().setPublisherPassword(
					new BigInteger(130, new SecureRandom()).toString(32));

		FileUtil.writeStringToFile(new File(externalDir + CONFIG_FILE_NAME), toJson());
	}

	public GeneralConfig getGeneralConfig() {
		return generalConfig;
	}

	public void setGeneralConfig(GeneralConfig generalConfig) {
		this.generalConfig = generalConfig;
	}

	public ToolConfig getToolConfigById(String toolId, String version) {
		ToolConfig toolConfig = null;
		// find organism config
		for (ToolConfig c: getTools())
			if (c.getUniqueToolId() != null) {
				ToolManifest m = c.getToolMenifest();
				if (m.getId().equals(toolId) && m.getVersion().equals(version))
					toolConfig = c;
			}
		return toolConfig;
	}

	public ToolManifest getToolManifestById(String toolId, String version) {
		ToolConfig toolConfigById = getToolConfigById(toolId, version);
		if (toolConfigById != null)
			return toolConfigById.getToolMenifest();
		else
			return null;
	}
	
	public ToolConfig getLastPublishedToolConfigById(String toolId) {
		ToolConfig ans = null;
		// find organism config
		for (ToolConfig c: getTools())
			if (c.getToolMenifest() != null) {
				ToolManifest m = c.getToolMenifest();
				if (m.getId().equals(toolId) 
						&& m.getPublicationDate() != null
						&& (ans == null 
							|| m.getPublicationDate().compareTo(
								ans.getToolMenifest().getPublicationDate()) > 0))
					ans = c;
			}
		return ans;
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

	public ToolConfig getToolConfigByConfiguration(String xmlDir) {
		if (xmlDir.isEmpty())
			return null;

		ToolConfig toolConfig = null;
		for (ToolConfig c: getTools())
			if (FileUtil.isSameFile(c.getConfiguration(), xmlDir))
				toolConfig = c;

		return toolConfig;
	}

	public List<ToolConfig> getTools() {
		return tools;
	}

	public void putTool(ToolConfig tool) {
		ToolConfig sameConfig = getToolConfigByConfiguration(tool.getConfiguration());
		if (getToolConfigByConfiguration(tool.getConfiguration()) != null)
			tools.remove(sameConfig);

		tools.add(tool);
	}

	public List<ToolManifest> getManifests(){
		List<ToolManifest> ans = new ArrayList<ToolManifest>();
		for (ToolConfig c: tools){
			ToolManifest m = c.getToolMenifest();
			if (m != null)
				ans.add(m);
		}
		return ans;
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
		private String publisherName; // Unique publisher name for the server copied to ToolManifest.
		private String publisherPassword; // Unique publisher name for the server created with GeneralConfig. used by Repo server and also sored there.
		private String repoUrl; // url of repository server.
		private String adminPassword;
		
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
		public String getPublisherName() {
			return publisherName;
		}
		public void setPublisherName(String publisherName) {
			this.publisherName = publisherName;
		}
		public String getPublisherPassword() {
			return publisherPassword;
		}
		public void setPublisherPassword(String publisherPassword) {
			this.publisherPassword = publisherPassword;
		}
		public String getRepoUrl() {
			//default
			if (repoUrl == null || repoUrl.isEmpty())
				return "http://typingtools.emweb.be/repository";
			return repoUrl;
		}
		public void setRepoUrl(String repoUrl) {
			this.repoUrl = repoUrl;
		}
		public String getAdminPassword() {
			return adminPassword;
		}
		public void setAdminPassword(String adminPassword) {
			this.adminPassword = adminPassword;
		}
	}

	public static class ToolConfig {
		private String path = new String(); // unique url path component that defines the tool (default toolId)
		private String configuration = new String(); // xmlPath - the directory that contains all the tool files.
		private String jobDir = new String(); // will contain all the work dirs of the tool. (Generated analysis data)
		private boolean autoUpdate;
		private boolean webService;
		private boolean ui;
		// ToolMenifest read manifests from configuration dir.
		// TODO: ui will have to update manifest if it was changed.
		transient private ToolManifest manifest = null;

		public ToolConfig copy() {
			ToolConfig c = new ToolConfig();
			c.autoUpdate = autoUpdate;
			c.webService = webService;
			c.ui = ui;

			return c;
		}

		/**
		 * create and set job, xml dirs for a new tool.
		 */
		public void genetareDirs() {
			genetareJobDir();
			genetareConfigurationDir();
		}

		public void genetareConfigurationDir() {
			try {
				File toolDir = FileUtil.createTempDirectory("tool-dir", 
						new File(Settings.getInstance().getBaseXmlDir()));
				setConfiguration(toolDir + File.separator);
			} catch (IOException e) {
				e.printStackTrace();
				assert(false); 
			}
		}
		
		public void genetareJobDir() {
			try {
				File toolJobDir = FileUtil.createTempDirectory("tool-dir", 
						new File(Settings.getInstance().getBaseJobDir()));
				setJobDir(toolJobDir + File.separator);
			} catch (IOException e) {
				e.printStackTrace();
				assert(false); 
			}
		}

		public ToolManifest getToolMenifest() {
			File f = new File(configuration + File.separator + "manifest.json");
			if (f.exists() && manifest == null) {
				String json = FileUtil.readFile(f);
				manifest = ToolManifest.parseJson(json);
			}
			return manifest;
		}

		public void invalidateToolManifest() {
			manifest = null;
		}

		public String getUniqueToolId() {
			return getToolMenifest() == null ? null : getToolMenifest().getUniqueToolId();
		}
		public String getId() {
			return getToolMenifest() == null ? null : getToolMenifest().getId();
		}
		public String getVersion() {
			return getToolMenifest() == null ? null : getToolMenifest().getVersion();
		}
		public String getPath() {
			if (path != null)
				return path;
			else
				return getToolMenifest() == null ? null : getToolMenifest().getUniqueToolId();
		}
		public void setPath(String path) {
			this.path = path;
		}
		public File getConfigurationFile() {
			return new File(configuration);
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
