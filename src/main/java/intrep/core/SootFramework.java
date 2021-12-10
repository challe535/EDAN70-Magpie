package intrep.core;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;

import intrep.util.magpiebridge.converter.WalaToSootIRConverter;
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
  private Collection<? extends Module> files;

  @Override
  public void setup(Collection<? extends Module> files, Set<String> classPath, Set<String> srcPath, Set<String> libPath,
      Set<String> progPath) {
    this.srcPath = srcPath;
    this.libPath = libPath;
    this.files = files;

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

    return results;
  }

  @Override
  public String frameworkName() {
    return "Soot";
  }
}
