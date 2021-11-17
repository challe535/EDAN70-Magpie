package intrep.core;

import java.net.URL;
import java.util.Collection;

import org.extendj.ast.CompilationUnit;
import magpiebridge.core.AnalysisResult;

public interface CodeAnalysis {
    
    public void doAnalysis(CompilationUnit cu, URL url);

    public Collection<AnalysisResult> getResult();
}
