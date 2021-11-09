import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.extendj.ast.CompilationUnit;
import org.extendj.ast.Problem;
import org.extendj.ast.Program;
// import org.extendj.ast.WarningMsg;

import beaver.Symbol;

import org.eclipse.lsp4j.DiagnosticSeverity;
// import org.extendj.ast.Analysis;

import org.extendj.ast.ASTNode;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.util.collections.Pair;

import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;
import magpiebridge.util.SourceCodeReader;

public class StringEqAnalysis {
  private static final Logger LOG = Logger.getLogger("main");
  private Collection<AnalysisResult> results;

  public StringEqAnalysis() {
      results = new HashSet<>();
      LOG.info("new analysis created");
  }

  public void doAnalysis(CompilationUnit cu) {
    Collection<Problem> probs = cu.warnings();

    LOG.info("Doing analysis");
    LOG.info("Nr of parseErrors = " + cu.parseErrors().size());
    LOG.info("Nr of semErrors = " + cu.errors().size());
    LOG.info("Nr of warnings = " + cu.warnings().size());

    for (Problem p : probs) {
        String type = p.message().split("::")[0];

        LOG.info(p.message());
        
        if(type.equals("StringEqCheck")) {
            LOG.info("Result found");

            ResultPosition position = new ResultPosition(p.line(), p.endLine(), p.column(), p.endColumn(), cu.pathName());
            List<Pair<Position, String>> relatedInfo = new ArrayList<>();

            String code = "no code";
            try {
                code = SourceCodeReader.getLinesInString(position);
            } catch (Exception e) {
                LOG.warning("Error retrieving code from source file");
                e.printStackTrace();
            }
            
            results.add(new Result(Kind.Diagnostic, position, p.message(), relatedInfo, DiagnosticSeverity.Warning, null, code));
        }
    }
  }

  public Collection<AnalysisResult> getResult() {
      return results;
  }

  

}
