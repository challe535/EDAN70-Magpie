package intrep.core;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;

import magpiebridge.converter.WalaToSootIRConverter;
import magpiebridge.core.AnalysisResult;

import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.JimpleBody;
import soot.options.Options;

public class SootFramework extends AnalysisFramework {
  private Set<String> srcPath;
  private Set<String> libPath;

  @Override
  public void setup(Collection<? extends Module> files, Set<String> classPath, Set<String> srcPath, Set<String> libPath,
      Set<String> progPath) {
    this.srcPath = srcPath;
    this.libPath = libPath;

    G.reset();
    Options.v().set_soot_classpath(calculateClassPathString(classPath));
    Options.v().set_prepend_classpath(true);
    Options.v().set_keep_line_number(true);
    Options.v().set_keep_offset(true);
  }

  @Override
  public int run() {
    WalaToSootIRConverter converter = new WalaToSootIRConverter(srcPath, libPath, null);
    converter.convert();
    return 0;
  }

  @Override
  public Collection<AnalysisResult> analyze(SourceFileModule file, URL clientURL, CodeAnalysis analysis) {
    HashSet<AnalysisResult> results = new HashSet<>();

    SootClass sc = Scene.v().getSootClass(file.getClassName());

    sc.setApplicationClass();
    Scene.v().loadNecessaryClasses();

    for (SootMethod sm : sc.getMethods()) {
      JimpleBody body = (JimpleBody) sm.retrieveActiveBody();
      analysis.doAnalysis(body, clientURL);
      results.addAll(analysis.getResult());
    }

    // SimpleTransformer transformer = new SimpleTransformer(analysis, clientURL);
    // runSootPacks(transformer);

    // return transformer.results;
    return results;
  }

  // private void runSootPacks(Transformer t) {
  // Transform transform = new Transform("jtp.analysis", t);
  // PackManager.v().getPack("jtp").add(transform);
  // PackManager.v().runBodyPacks();
  // }

  @Override
  public String frameworkName() {
    return "Soot";
  }

  // private class SimpleTransformer extends BodyTransformer {
  // private Collection<AnalysisResult> results;
  // private CodeAnalysis<Body> analysis;
  // private URL clientURL;

  // public SimpleTransformer(CodeAnalysis<Body> analysis, URL clientURL) {
  // results = new HashSet<>();
  // this.analysis = analysis;
  // this.clientURL = clientURL;
  // }

  // // public Collection<AnalysisResult> getAnalysisResults() {
  // // return results;
  // // }

  // @Override
  // protected void internalTransform(Body b, String phaseName, Map<String,
  // String> options) {
  // analysis.doAnalysis(b, clientURL);
  // results.addAll(analysis.getResult());
  // }
  // }
}
