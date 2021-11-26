package intrep.analysis.soot.npa;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.lsp4j.DiagnosticSeverity;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;

import intrep.core.CodeAnalysis;
import intrep.core.MySourceCodeReader;
import intrep.core.Result;

import magpiebridge.converter.sourceinfo.StmtPositionInfo;
import magpiebridge.converter.tags.StmtPositionInfoTag;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;

import soot.Body;
import soot.Local;
import soot.Unit;
import soot.ValueBox;
import soot.toolkits.graph.TrapUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class SootNPAnalysis implements CodeAnalysis<Body> {
    private static final Logger LOG = Logger.getLogger("main");
    Collection<AnalysisResult> results;

    public SootNPAnalysis() {
        results = new HashSet<AnalysisResult>();
    }

    @Override
    public void doAnalysis(Body cu, URL url) {
        results.clear();
        UnitGraph graph = new TrapUnitGraph(cu);

        List<NullPointerAnalysis> npAnalyses = new ArrayList<>();
        npAnalyses.add(new NullPointerAnalysis(graph, NullPointerAnalysis.AnalysisMode.MUST));
        npAnalyses.add(new NullPointerAnalysis(graph, NullPointerAnalysis.AnalysisMode.MAY_O));
        npAnalyses.add(new NullPointerAnalysis(graph, NullPointerAnalysis.AnalysisMode.MAY_P));

        LOG.info("Completed SootNPA analyses");

        int npWarnCount = 0;
        for(Unit unit : cu.getUnits()){
            for(ValueBox usedValueBox : unit.getUseBoxes()){
                if(usedValueBox.getValue() instanceof Local){
                    Local usedLocal = (Local) usedValueBox.getValue();
                    for(NullPointerAnalysis npa: npAnalyses){
                        if(npa.getFlowBefore(unit).contains(usedLocal)){
                            StmtPositionInfo pos = ((StmtPositionInfoTag)unit.getTag("StmtPositionInfoTag")).getStmtPositionInfo();
                            String code = "no code";
                            try {
                                code = MySourceCodeReader.getLinesInString(pos.getStmtPosition());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            String msg = "Soot::Possible NullPointer usage; Variable " + usedLocal.getName() + " may be null";
                            List<Pair<Position, String>> relatedInfo = new ArrayList<>();

                            LOG.info("NP found on:\n" +
                                     "url: " + url + "\n" +
                                     "lineStart: " + pos.getStmtPosition().getFirstLine() +"\n"+
                                     "colStart: " + pos.getStmtPosition().getFirstCol() +"\n"+
                                     "lineEnd: " + pos.getStmtPosition().getLastLine() +"\n"+
                                     "colEnd: " + pos.getStmtPosition().getLastCol() +"\n"
                                    );
                            
                            results.add(new Result(Kind.Diagnostic, pos.getStmtPosition(), msg, relatedInfo, DiagnosticSeverity.Warning, null, code));

                            npWarnCount++;
                        }
                    }
                }
            }
        }

        LOG.info("Ended SootNPAnalysis with " + npWarnCount + " warnings");
    }

    @Override
    public Collection<AnalysisResult> getResult() {
        return results;
    }

    @Override
    public String getName() {
        return "SootNPA";
    }
    
}
