/*
 * Created on Feb 6, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.viruses.hiv;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.ScanAnalysis;
import rega.genotype.PhyloClusterAnalysis.Result;

public class HIV1SubtypeTool extends GenotypeTool {
    private AlignmentAnalyses hiv1;
    private PhyloClusterAnalysis pureAnalysis;
    private ScanAnalysis scanAnalysis;
    private PhyloClusterAnalysis crfAnalysis;
    private PhyloClusterAnalysis purePuzzleAnalysis;

    private ScanAnalysis crfScanAnalysis;
    private PhyloClusterAnalysis crfScanPhyloAnalysis;

    public HIV1SubtypeTool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
        hiv1 = readAnalyses("HIV/hiv1.xml", workingDir);
        pureAnalysis = (PhyloClusterAnalysis) hiv1.getAnalysis("pure");
        purePuzzleAnalysis = (PhyloClusterAnalysis) hiv1.getAnalysis("pure-puzzle");
        scanAnalysis = (ScanAnalysis) hiv1.getAnalysis("scan");
        crfScanAnalysis = (ScanAnalysis) hiv1.getAnalysis("crfscan");
        crfScanPhyloAnalysis = (PhyloClusterAnalysis) crfScanAnalysis.getAnalysis();
        crfAnalysis = (PhyloClusterAnalysis) hiv1.getAnalysis("crf");
    }

    public void analyze(AbstractSequence s) throws AnalysisException {
        if (s.getLength() > 800) {
            PhyloClusterAnalysis.Result pureResult = pureAnalysis.run(s);
            ScanAnalysis.Result scanResult = scanAnalysis.run(s);
            PhyloClusterAnalysis.Result crfResult = crfAnalysis.run(s);
            ScanAnalysis.Result scanCRFResult = null;
            PhyloClusterAnalysis.Result crfPhyloResult = null;
            double scanCRFSupport = 0;

            if (crfResult.haveSupport() && crfResult.getBestCluster().hasTag("CRF")) {
                crfScanPhyloAnalysis.getClusters().add(crfResult.getBestCluster());

                scanCRFResult = crfScanAnalysis.run(s);
                scanCRFSupport = scanCRFResult.getBootscanSupport(crfResult.getBestCluster().getId());
                
                pureAnalysis.getClusters().add(crfResult.getBestCluster());
                crfPhyloResult = pureAnalysis.run(s);

                crfScanPhyloAnalysis.getClusters().remove(crfResult.getBestCluster());
                pureAnalysis.getClusters().remove(crfResult.getBestCluster());
            }

            if (scanResult.haveSupport()) {
                rule1(pureResult, crfResult);
            } else {                
                if (!crfResult.haveSupport()) {
                	Map<String,Float> supported = scanResult.getSupportedTypes();
                	
                	if (supported.size() >= 2) {
                		//Rule 2A
                		conclude("Recombinant "+ toRecombinantString(supported),
                				"Rule 2A");
                		return;
                	} else if (supported.size() == 1) {
                		Map.Entry<String, Float> support = supported.entrySet().iterator().next();
                		
                		if (support.getValue() >= 0.7) {
                			//Rule 2B goto Rule 1
                			rule2B(pureResult, crfResult);
                		} else if (support.getValue() >= 0.5) {
                			//Rule 2C
                			conclude(support.getValue()+", potential recombinant",
                					"Rule 2C");
                			return;
                		}
                	}
                	//Rule 2D
                    conclude("Check the bootscan",
                            "Subtype unassigned based on sequence > 800 bps " +
                            "clustering with a pure subtype with bootstrap > 70% " +
                            "with detection of recombination in the bootscan, " +
                            "and failure to classify as a CRF or sub-subtype (bootstrap support).");
                } else {
                    if (crfResult.getBestCluster().hasTag("CRF")) {
                    	Map<String,Float> supported = scanCRFResult.getSupportedTypes();
                    	
                        if (!scanCRFResult.haveSupport()) {
                        	if( scanCRFSupport > 0.1 && supported.size() >= 1) {
                        		//Rule 3A
                        		conclude("Recombinant "+ toRecombinantString(supported),
                        				"Rule 3A");
                        	} else if (scanCRFSupport > 0.7 && supported.size() == 0) {
                        		//Rule 3B goto Rule 1
                        		rule3B(crfPhyloResult);
                        	} else if (scanCRFSupport > 0.5 && supported.size() == 0) {
                        		//Rule 3C
                        		conclude(crfResult,"potential recombinant subtype",
                        				"Rule 3C");
                        	} else {
                        		//Rule 3D
                                conclude("Check the bootscan",
                                        "Subtype unassigned based on sequence > 800bp, " +
                                        "clustering with a pure subtype and CRF or sub-subtype with bootstrap >70 %, " +
                                        "with detection of recombination in the pure subtype bootscan, " +
                                        "and failure to classify as a CRF or sub-subtype by bootscan analysis.");
                        	}

                        } else {
                            // Rule 4-8
                            conclude(crfResult,
                                    "Subtype assigned based on sequence > 800bp, " +
                                    "clustering with a CRF or sub-subtype with bootstrap > 70%, " +
                                    "with detection of recombination in the pure subtype bootscan, " +
                                    "and further confirmed as a CRF or sub-subtype by bootscan analysis.");
                        }
                    } else {
                        conclude("Check the bootscan", 
                        		  	"Subtype unassigned based on sequence > 800bp, " +
                        			"not clustering with a pure subtype with bootstrap >70 %," +
                        			"with or without detection of recombination in the pure subtype bootscan");
                    }
                }
            }
        } else {
            PhyloClusterAnalysis.Result pure = purePuzzleAnalysis.run(s);
            
            if (!pure.haveSupport()) {
                // Rule 9
                conclude("Check the report",
                        "Subtype unassigned based on sequence &lt; 800bp, " +
                        "and not clustering with a pure subtype with bootstrap >70 %.");
            } else {
                if ((pure.getSupportInner() - pure.getSupportOuter()) > -50) {
                    // Rule 11
                    conclude(pure,
                            "Subtype assigned based on sequence &lt; 800bp, " +
                            "clustering with a pure subtype with bootstrap > 70%, " +
                            "and clustering inside the pure subtype cluster.");
                } else {
                    // Rule 10
                    conclude("Check the report",
                            "Subtype assigned based on sequence &lt; 800bp, " +
                            "clustering with a pure subtype with bootstrap > 70%, " +
                            "however not clustering inside the pure subtype cluster.");
                }
            }
        }
    }
    
    private void rule1(Result pureResult, Result crfResult){
    	pureRules(pureResult, crfResult, true);
    }
    private void rule3B(Result crfPhyloResult){
    	pureRules(crfPhyloResult, null, true);
    }
    private void rule2B(Result pureResult, Result crfResult){
    	pureRules(pureResult, crfResult, false);
    }
    private void pureRules(Result pureResult, Result crfResult, boolean clearlyNoRecombination){
    	String recombinationConclusion = clearlyNoRecombination ? 
    			"without recombination in the bootscan."
    			: "without significant recombination in the bootscan.";
    	
    	if (pureResult.haveSupport()) {
            if (crfResult != null
            	&& crfResult.haveSupport()
                && (crfResult.getSupportInner() > crfResult.getSupportOuter())) {
                // Rule 1C (CRF)
                conclude(pureResult,crfResult,
                    "Subtype assigned based on sequence > 800 bps " +
                    "clustering with a pure subtype and CRF or sub-subtype with bootstrap > 70% " +
                    recombinationConclusion);  
            } else if (pureResult.getSupportInner() > pureResult.getSupportOuter()) {
                conclude(pureResult,
                        "Subtype assigned based on sequence > 800 bps "
                        + "clustering with a pure subtype with bootstrap > 70% "
                        + recombinationConclusion);
            	// Rule 1a (pure)
            } else if (pureResult.getSupportInner() > pureResult.getSupportOuter() - 30) {
            	// Rule 1b
            	conclude(pureResult,"Rule 1B: pure like");
            } else {
                // Rule 1c
                conclude("Check the Report",
                        "Subtype unassigned based on sequence > 800 bps " +
                        "failure to classify as pure subtype (Bootstrap Support)" +
                        recombinationConclusion);
            }
        } else {
        	//Rule 5
            conclude("Check the Report",
               "Subtype unassigned based on sequence > 800 bps " +
               "failure to classify as pure subtype (Bootstrap Support)" +
               recombinationConclusion);
        }
    }
    
    private String toRecombinantString(Map<String,Float> m){
    	StringBuilder sb = new StringBuilder();
    	
    	boolean first = true;
    	for(Map.Entry<String, Float> me : m.entrySet()){
    		if(!first)
    			sb.append(", ");
    		else
    			first = false;
    		
    		sb.append(me.getKey() +" ("+ (int)(me.getValue()*100) +"%)");
    	}
    	
    	return sb.toString();
    }

    public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) hiv1.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
    }

}

