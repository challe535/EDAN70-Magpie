package intrep.analysis;

import intrep.analysis.soot.npa.SootNPAnalysis;
import intrep.core.magpiebridge.StaticServerAnalysis;

public class AnalysisInjector {
    public static void initAnalysis() {
        //IntraJ
        StaticServerAnalysis.addAnalysis(new StringEqAnalysis());
        StaticServerAnalysis.addAnalysis(new DAAnalysis());
        StaticServerAnalysis.addAnalysis(new NPAnalysis());
        
        //Soot
        StaticServerAnalysis.addAnalysis(new SootNPAnalysis());
    }
}
