import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.extendj.ast.CompilationUnit;
import org.extendj.ast.Program;
import org.extendj.ast.WarningMsg;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.extendj.ast.Analysis;

import org.extendj.ast.ASTNode;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.util.collections.Pair;

import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;

public class StringEqAnalysis {
    private Collection<AnalysisResult> result;

    public StringEqAnalysis() {
        result = new HashSet<>();
        System.out.println("new analysis created");
    }

    public void doAnalysis(Program p) {
        Analysis analysis = Analysis.DAA;

        try {
            for (CompilationUnit cu : p.getCompilationUnits()) {
              TreeSet<WarningMsg> wmgs = (TreeSet<WarningMsg>)cu.getClass()
                                             .getDeclaredMethod(analysis.toString())
                                             .invoke(cu);
              for (WarningMsg wm : wmgs) {
                if (analysis.equals(wm.getAnalysisType())) {
                  wm.print(System.out);

                  result.add(new Result(Kind.Diagnostic,  , wm.toString(), null, DiagnosticSeverity.Error, null, "some code"));
                }
              }
            }
          } catch (Throwable t) {
            System.out.println(t.getMessage());
          }
    }

    public Collection<AnalysisResult> getResult() {
        return result;
    }

}
