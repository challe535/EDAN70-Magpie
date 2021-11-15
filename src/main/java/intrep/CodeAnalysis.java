package intrep;

import java.util.Collection;

import org.extendj.ast.CompilationUnit;
import magpiebridge.core.AnalysisResult;

public interface CodeAnalysis {
    
    public void doAnalysis(CompilationUnit cu);

    public Collection<AnalysisResult> getResult();
}
