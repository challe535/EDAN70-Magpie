import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.extendj.ast.CompilationUnit;
import org.extendj.ast.Problem;

import org.eclipse.lsp4j.DiagnosticSeverity;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;

import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;

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

            ResultPosition position = new ResultPosition(p.line(), p.endLine(), p.column()-1, p.endColumn(), cu.pathName());
            List<Pair<Position, String>> relatedInfo = new ArrayList<>();

            String code = "no code";
            try {
                code = MySourceCodeReader.getLinesInString(position);
            } catch (Exception e) {
                LOG.warning("Error retrieving code from source file");
                e.printStackTrace();
            }

            String[] lr = code.split("==");
            
            for(int i = 0; i < lr.length; i++) {
                lr[i] = lr[i].trim();
                lr[i] = lr[i].replace("\n", "").replace("\r", "");
            }

            String correctCode = lr[0] + ".equals(" + lr[1] + ")";

            Pair<Position, String> repair = Pair.make(position, correctCode);
            relatedInfo.add(repair);
            
            results.add(new Result(Kind.Diagnostic, position, p.message(), relatedInfo, DiagnosticSeverity.Warning, repair, code));
        }
    }
  }

  public Collection<AnalysisResult> getResult() {
      return results;
  }

  

}
