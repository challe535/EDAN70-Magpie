package intrep.core;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;

import magpiebridge.converter.WalaToSootIRConverter;
import magpiebridge.core.AnalysisResult;
import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.JimpleBody;
import soot.options.Options;

public class SootFramework implements AnalysisFramework {
    private static final Logger LOG = Logger.getLogger("main");
    Set<String> srcPath;
    Set<String> libPath;

    @Override
    public void setup(Collection<? extends Module> files, String classPath, Set<String> srcPath, Set<String> libPath, Set<String> progPath) {
        this.srcPath = srcPath;
        this.libPath = libPath;
        
        G.reset();
        Options.v().set_soot_classpath(classPath);
        Options.v().set_prepend_classpath(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_keep_offset(true);
    }

    @Override
    public int run() {
        return 0;
    }

    @Override
    public Collection<AnalysisResult> analyze(SourceFileModule file, URL clientURL, CodeAnalysis analysis) {
      // Uses the IRConverter to convert WALA IR to Jimple.
      WalaToSootIRConverter converter = new WalaToSootIRConverter(srcPath, libPath, null);
      converter.convert();

      HashSet<AnalysisResult> results = new HashSet<>();
      SootClass sc = Scene.v().getSootClass(file.getClassName());
      for(SootMethod sm : sc.getMethods()) {
        JimpleBody body = (JimpleBody) sm.retrieveActiveBody();
        analysis.doAnalysis(body, clientURL);
        results.addAll(analysis.getResult());
      }
      return results;
    }

    private class SimpleTransformer extends BodyTransformer {
      private Collection<AnalysisResult> results;
      private CodeAnalysis<Body> analysis;
      private URL clientURL;

      public SimpleTransformer(CodeAnalysis<Body> analysis, URL clientURL) {
        results = new HashSet<>();
        this.analysis = analysis;
        this.clientURL = clientURL;
      }
    
      public Collection<AnalysisResult> getAnalysisResults() {
        return results;
      }
    
      @Override
      protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        analysis.doAnalysis(b, clientURL);
        results.addAll(analysis.getResult());
      }
    }
    
}
