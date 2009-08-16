/*
 * Created on Feb 8, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.hcv;

import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;

public class HCVToolGeno extends GenotypeTool {

    private AlignmentAnalyses hcv;
    private BlastAnalysis blastAnalysis;
    private HCVSubtypeToolGeno hcvsubtypetool;

    
    public HCVToolGeno() throws IOException, ParameterProblemException, FileFormatException {
        hcv = readAnalyses("hcvblast.xml");
        blastAnalysis = (BlastAnalysis) hcv.getAnalysis("blast");
        
        hcvsubtypetool = new HCVSubtypeToolGeno();
        hcvsubtypetool.setParent(this);

    }

    public void analyze(AbstractSequence s) throws AnalysisException {
        BlastAnalysis.Result result = blastAnalysis.run(s);
        
        if (result.haveSupport()) {
            if (result.getCluster().getId().equals("1"))
                hcvsubtypetool.analyze(s);
            else
                conclude(result, "Identified with BLAST score > 200");
        } else {

            conclude("Unassigned", "Unassigned because of BLAST score &lt; 200.");
        }
    }

	public void analyzeSelf() throws AnalysisException {
	}
}

