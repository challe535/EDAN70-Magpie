package intrep.core;

import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;

import org.extendj.IntraJ;
import org.extendj.ast.CompilationUnit;

import magpiebridge.core.AnalysisResult;

public class IntraJFramework implements AnalysisFramework {
  private IntraJ jChecker;
  private Collection<String> args;

  @Override
  public void setup(Collection<? extends Module> files, String classPath, Set<String> srcPath, Set<String> libPath, Set<String> progPath) {
    jChecker = new IntraJ();
    args = new LinkedHashSet<String>();

    args.add("-nowarn");

    //totalClassPath += ";" + srcPath;

    if(!classPath.equals("")) {
      args.add("-classpath");
      args.add(classPath);
    }

    for (String path : progPath) {
      args.add(path);
    }

    for (Module file : files) {
      if (file instanceof SourceFileModule) {
        SourceFileModule sourceFile = (SourceFileModule) file;
        
        args.add(sourceFile.getAbsolutePath());
      }
    }
  }

  @Override
  public int run() {
    return jChecker.run(args.toArray(new String[args.size()]));
  }

  @Override
  public Collection<AnalysisResult> analyze(SourceFileModule file, URL clientURL, CodeAnalysis analysis) {
    for (CompilationUnit cu : jChecker.getEntryPoint().getCompilationUnits()) {
      if(cu.getClassSource().sourceName().equals(file.getAbsolutePath())) {
        analysis.doAnalysis(cu, clientURL);
      }
    }
  
    return analysis.getResult();
  }
    
}
