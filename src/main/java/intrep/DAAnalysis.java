package intrep;

import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.extendj.ast.Analysis;
import org.extendj.ast.CFGNode;
import org.extendj.ast.CFGRoot;
import org.extendj.ast.CompilationUnit;
import org.extendj.ast.MethodDecl;
import org.extendj.ast.Program;
import org.extendj.ast.WarningMsg;
import org.extendj.flow.utils.IJGraph;
import org.extendj.flow.utils.Utils;

import magpiebridge.core.AnalysisResult;

public class DAAnalysis implements CodeAnalysis {
    private static final Logger LOG = Logger.getLogger("main");
    private Collection<AnalysisResult> results;

    private static final Analysis ANALYSIS_TYPE = Analysis.DAA;

    public DAAnalysis() {
        results = new HashSet<>();
    }

    @Override
    public void doAnalysis(CompilationUnit cu) {
        try {
            TreeSet<WarningMsg> wmgs = (TreeSet<WarningMsg>)cu.getClass()
            .getDeclaredMethod(ANALYSIS_TYPE.toString())
            .invoke(cu);     
        } catch (Throwable t) {
        }   
    }

    @Override
    public Collection<AnalysisResult> getResult() {
        return results;
    }
    
}
