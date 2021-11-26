package intrep.analysis;

import intrep.analysis.soot.npa.SootNPAnalysis;
import intrep.core.StaticServerAnalysis;

public class AnalysisInjector {
    public static void initAnalysis() {
        StaticServerAnalysis.addAnalysis(new StringEqAnalysis());
        StaticServerAnalysis.addAnalysis(new DAAnalysis());
        StaticServerAnalysis.addAnalysis(new NPAnalysis());
        // StaticServerAnalysis.addAnalysis(new SootNPAnalysis());
    }
}
