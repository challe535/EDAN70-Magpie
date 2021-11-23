package intrep.analysis;

import intrep.core.CodeAnalysis;
import intrep.core.MySourceCodeReader;
import intrep.core.Result;
import intrep.core.ResultPosition;

import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.List;
import java.net.URL;
import java.util.ArrayList;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;

import org.eclipse.lsp4j.DiagnosticSeverity;

import org.extendj.ast.Analysis;
import org.extendj.ast.CompilationUnit;
import org.extendj.ast.MethodDecl;
import org.extendj.ast.WarningMsg;

import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;

public class NPAnalysis implements CodeAnalysis {
    private Collection<AnalysisResult> results;

    private static final Analysis ANALYSIS_TYPE = Analysis.NPA;

    public NPAnalysis() {
        results = new HashSet<>();
    }

    @Override
    public void doAnalysis(CompilationUnit cu, URL url) {
        results.clear();

        try {
            TreeSet<WarningMsg> wmgs = (TreeSet<WarningMsg>)cu.getClass()
                                        .getDeclaredMethod(ANALYSIS_TYPE.toString())
                                        .invoke(cu);     

            for (WarningMsg wm : wmgs) {

                ResultPosition position = new ResultPosition(wm.lineStart, wm.lineEnd, wm.columnStart, wm.columnEnd, url);
                List<Pair<Position, String>> relatedInfo = new ArrayList<>();

                String code = "no code";
                try {
                    code = MySourceCodeReader.getLinesInString(position);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                results.add(new Result(Kind.Diagnostic, position, wm.errMsg, relatedInfo, DiagnosticSeverity.Warning, null, code));
            }
        } catch (Throwable t) {
        }   
    }

    @Override
    public Collection<AnalysisResult> getResult() {
        return results;
    }

    @Override
    public String getName() {
        return "NPA";
    }
}
