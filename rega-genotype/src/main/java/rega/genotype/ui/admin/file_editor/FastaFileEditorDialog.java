package rega.genotype.ui.admin.file_editor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.framework.widgets.ObjectListComboBox;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Utils;
import rega.genotype.viruses.generic.GenericTool;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WContainerWidget.Overflow;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextArea;

/**
 * Editor for blast.fasta file
 * 
 * @author michael
 */
public class FastaFileEditorDialog extends WDialog{
	private TaxusTable analysFastaFileWidget;
	public FastaFileEditorDialog(final Cluster cluster, final AlignmentAnalyses alignmentAnalyses,
			final ToolConfig toolConfig) {
		// TODO: if not blast tool run blust tool to identify the clusters.
		show();
		getTitleBar().addWidget(new WText("Add sequences"));
		setHeight(new WLength(400));
		setResizable(true);
		getContents().setOverflow(Overflow.OverflowAuto);

		final WPushButton nextB = new WPushButton("Next", getFooter());
		final WPushButton okB = new WPushButton("OK", getFooter());
		final WPushButton cancelB = new WPushButton("Cancel", getFooter());

		final FastaFileUpload fastaFileUpload = new FastaFileUpload();
		getContents().addWidget(fastaFileUpload);

		okB.hide();

		nextB.clicked().addListener(nextB, new Signal.Listener() {
			public void trigger() {
				analysFastaFileWidget = new TaxusTable(
						cluster, fastaFileUpload.getText(), alignmentAnalyses, toolConfig);
				WContainerWidget c = new WContainerWidget();
				c.addWidget(analysFastaFileWidget);
				getContents().removeWidget(fastaFileUpload);
				getContents().addWidget(analysFastaFileWidget);
				nextB.hide();
				okB.show();
			}
		});

		okB.clicked().addListener(okB, new Signal.Listener() {
			public void trigger() {
				accept();
			}
		});
		cancelB.clicked().addListener(cancelB, new Signal.Listener() {
			public void trigger() {
				reject();
			}
		});
	}

	public Map<AbstractSequence, Cluster> getSelectedSequences() {
		if (analysFastaFileWidget == null)
			return new HashMap<AbstractSequence, Cluster>();
		else
			return analysFastaFileWidget.getSelectedSequences();
	}

	// classes

	// step 1: choose fasta file 

	public static class FastaFileUpload extends WContainerWidget {
		final WTextArea fastaTA = new WTextArea(this);
		final FileUpload upload = new FileUpload();
		public FastaFileUpload() {
			fastaTA.setInline(false);
			fastaTA.setWidth(new WLength(700));
			fastaTA.setHeight(new WLength(300));

			upload.getWFileUpload().setFilters(".fasta");
			addWidget(upload);
			upload.uploadedFile().addListener(upload, new Signal1.Listener<File>() {
				public void trigger(File file) {
					fastaTA.setText(FileUtil.readFile(file));
				}
			});
		}

		public String getText(){
			return fastaTA.getText();
		}
	}

	// step 2: choose clusters 

	private static class SequenceData {
		WCheckBox addSequenceChB;
		ObjectListComboBox<Cluster> clusterCB;
		public SequenceData(WCheckBox addSequenceChB, ObjectListComboBox<Cluster> clusterCB) {
			this.addSequenceChB = addSequenceChB;
			this.clusterCB = clusterCB;
		}
	}

	private static class TaxusTable extends WTable { 
		private Map<AbstractSequence, SequenceData> sequenceMap = new HashMap<AbstractSequence, SequenceData>();
		private AlignmentAnalyses alignmentAnalyses; // AlignmentAnalyses of the tool
		private SequenceAlignment currentSeqAlign; // Sequences from the input fasta file
		private List<String> taxaIds = new ArrayList<String>(); // pre-compute from alignmentAnalyses

		private enum Mode {SingalCluster, AllClusters};
		private Mode mode = Mode.SingalCluster;
		
		public TaxusTable(Cluster cluster, String fasta,
				AlignmentAnalyses alignmentAnalyses, ToolConfig toolConfig) {
			this.alignmentAnalyses = alignmentAnalyses;

			// pre-compute taxaIds
			for (Cluster c: alignmentAnalyses.getAllClusters()){
				taxaIds.addAll(c.getTaxaIds());
			}

			// headers 
			setHeaderCount(1);
			getElementAt(0, 0).addWidget(new WText("Add"));
			getElementAt(0, 1).addWidget(new WText("Name"));
			getElementAt(0, 2).addWidget(new WText("Tool"));
			getElementAt(0, 3).addWidget(new WText("Cluster"));
			getElementAt(0, 4).addWidget(new WText("Info"));

			addStyleClass("fasta-analysis-table");
			setHeight(new WLength(300));

			alignmentAnalyses.analyses();

			if (cluster == null) {// add sequences to many clusters.
				mode = Mode.AllClusters;
				// run the tool to identify the clusters.
				final File jobDir = GenotypeLib.createJobDir(toolConfig.getJobDir());
				try {
					InputStream stream = new ByteArrayInputStream(fasta.getBytes(StandardCharsets.UTF_8));
					currentSeqAlign = new SequenceAlignment(stream, 
							SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);

					stream.reset();
					GenericTool t = new GenericTool(toolConfig, jobDir);
					t.analyze(stream, jobDir.getAbsolutePath() + File.separator + "result.xml");
					new Parser().parseFile(jobDir);
				} catch (IOException e) {
					e.printStackTrace();
					fillTaxuesTable(cluster, fasta, alignmentAnalyses.getAlignment().getSequenceType());
				} catch (ParameterProblemException e) {
					e.printStackTrace();
					fillTaxuesTable(cluster, fasta, alignmentAnalyses.getAlignment().getSequenceType());
				} catch (FileFormatException e) {
					e.printStackTrace();
					fillTaxuesTable(cluster, fasta, alignmentAnalyses.getAlignment().getSequenceType());
				}

				// use result parser on blast-tool-result.xml
			} else // the cluster is pre selected
				fillTaxuesTable(cluster, fasta, alignmentAnalyses.getAlignment().getSequenceType());
		}

		/**
		 * Parse result.xml file from job dir and fill the output to blastResultModel. 
		 */
		private class Parser extends GenotypeResultParser {
			@Override
			public void endSequence() {	
				String clusterId = GenotypeLib.getEscapedValue(this, "/genotype_result/sequence/result[@id='blast']/cluster/id");
				String seqName = GenotypeLib.getEscapedValue(this, "/genotype_result/sequence/@name");
				String concludedId = GenotypeLib.getEscapedValue(this, "/genotype_result/sequence/result[@id='blast']/cluster/concluded-id");

				AbstractSequence s = currentSeqAlign.findSequence(seqName);

				Cluster cluster = concludedId.equals("Unassigned") ? null :
					alignmentAnalyses.findCluster(clusterId);

				addRow(s, cluster);
			}
		}

		private void addRow(AbstractSequence s, Cluster cluster) {
			int row = getRowCount();
			final WCheckBox chb = new WCheckBox();
			final WText clusterNameT = new WText("");

			// clusterCB
			List<Cluster> clusters = new ArrayList<AlignmentAnalyses.Cluster>(
					alignmentAnalyses.getAllClusters());
			clusters.add(0, null);

			final ObjectListComboBox<Cluster> clusterCB = new ObjectListComboBox<AlignmentAnalyses.Cluster>(
					clusters) {
				@Override
				protected WString render(Cluster c) {
					if (c == null)
						return new WString("(Empty)");
					else
						return new WString(c.getName());
				}
			};

			if (taxaIds != null && taxaIds.contains(s.getName())){
				chb.setUnChecked();
				chb.disable();
				getElementAt(row, 4).addWidget(new WText("Already exists"));
				clusterCB.disable();
			} else 
				chb.setChecked();
			if(mode == Mode.SingalCluster)
				clusterCB.disable();

			clusterCB.setCurrentObject(cluster);
			clusterCB.changed().addListener(clusterCB, new Signal.Listener() {
				public void trigger() {
					Cluster currentCluster = clusterCB.getCurrentObject();
					if (currentCluster == null) {
						clusterNameT.setText("");
						chb.setUnChecked();
						chb.disable();
					} else {
						clusterNameT.setText(Utils.nullToEmpty(currentCluster.getToolId()));
						chb.enable();
					}
				}
			});
			clusterCB.changed().trigger();

			// bind

			getElementAt(row, 0).addWidget(chb);
			getElementAt(row, 1).addWidget(new WText(Utils.nullToEmpty(s.getName())));
			getElementAt(row, 2).addWidget(clusterNameT);
			getElementAt(row, 3).addWidget(clusterCB);

			sequenceMap.put(s, new SequenceData(chb, clusterCB));
		}

		public void fillTaxuesTable(Cluster cluster, String fasta, int sequenceType) {
			SequenceAlignment alignment = parseFasta(fasta, sequenceType);
			if (alignment != null) {
				List<AbstractSequence> sequences = alignment.getSequences();
				for (AbstractSequence s: sequences) {
					addRow(s, cluster);
				}
			}
		}

		public Map<AbstractSequence, Cluster> getSelectedSequences() {
			Map<AbstractSequence, Cluster> ans = new HashMap<AbstractSequence, Cluster>();
			for (Map.Entry<AbstractSequence, SequenceData> e: sequenceMap.entrySet()){
				if (e.getValue().addSequenceChB.isChecked())
					ans.put(e.getKey(), e.getValue().clusterCB.getCurrentObject());
			}

			return ans;
		}

		private SequenceAlignment parseFasta(String fasta, int sequenceType) {
			try {
				InputStream stream = new ByteArrayInputStream(fasta.getBytes(StandardCharsets.UTF_8));
				SequenceAlignment alignment = new SequenceAlignment(stream,
						SequenceAlignment.FILETYPE_FASTA, sequenceType);
				return alignment;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (ParameterProblemException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (FileFormatException e) {
				e.printStackTrace();
			}

			return null;
		}
	}
}