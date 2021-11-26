package intrep.core;

import java.net.URL;
import java.util.Collection;
import java.util.Set;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;

import magpiebridge.core.AnalysisResult;

public interface AnalysisFramework {
    public void setup(Collection<? extends Module> files, String classPath, Set<String> srcPath, Set<String> libPath, Set<String> progPath);

    public int run();

    public Collection<AnalysisResult> analyze(SourceFileModule file, URL clientURL, CodeAnalysis analysis);
}
