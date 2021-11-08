import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Stream;

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
  private static final Logger LOG = Logger.getLogger("main");
  private Collection<AnalysisResult> result;

  public StringEqAnalysis() {
      result = new HashSet<>();
      LOG.info("new analysis created");
  }

  public void doAnalysis(Program p) {
      Analysis analysis = Analysis.DAA;

      try {
          for (CompilationUnit cu : p.getCompilationUnits()) {
            String cuPath = cu.pathName();

            LOG.info("CUPath = " + cuPath);

            TreeSet<WarningMsg> wmgs = (TreeSet<WarningMsg>)cu.getClass()
                                            .getDeclaredMethod(analysis.toString())
                                            .invoke(cu);
            for (WarningMsg wm : wmgs) {
              if (analysis.equals(wm.getAnalysisType())) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream s = new PrintStream(baos);
                
                wm.print(s);

                LOG.warning(baos.toString());

                List<Pair<Position, String>> relatedInfo = new ArrayList<>();
                result.add(new Result(Kind.Diagnostic, new ResultPosition(cuPath) , baos.toString(), relatedInfo, 
                                      DiagnosticSeverity.Error, null, "some code"));
              }
            }
          }
        } catch (Throwable t) {
          LOG.severe(t.getMessage());
        }
  }

  public Collection<AnalysisResult> getResult() {
      return result;
  }

}
