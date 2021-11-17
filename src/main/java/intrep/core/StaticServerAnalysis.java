package intrep.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceFileModule;

import org.extendj.JavaChecker;
import org.extendj.ast.CompilationUnit;
import org.extendj.ast.List;
import org.extendj.IntraJ;

import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.IProjectService;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.projectservice.java.JavaProjectService;
/**
 * 
 * @author Linghui Luo
 *
 */
public class StaticServerAnalysis implements ServerAnalysis {
  private static final Logger LOG = Logger.getLogger("main");
  private Set<String> srcPath;
  private Set<String> libPath;
  private Set<String> srcFilesAbsPaths;
  private String totalClassPath;
  private ExecutorService exeService;
  private Future<?> last;

  private static Collection<CodeAnalysis> analysisList;

  public StaticServerAnalysis() {
    exeService = Executors.newSingleThreadExecutor();
    analysisList = new ArrayList<CodeAnalysis>();
  }

  @Override
  public String source() {
    return "Interactive Repair";
  }

  @Override
  public void analyze(Collection<? extends Module> files, AnalysisConsumer consumer, boolean rerun) {
    if(rerun) {

      //COMPILE
      IntraJ jChecker  = new IntraJ();

      MagpieServer server = (MagpieServer) consumer;

      setClassPath(server);

      Collection<String> args = new ArrayList<String>();

      args.add("-classpath");
      args.add(totalClassPath);


      for (Module file : files) {
        if (file instanceof SourceFileModule) {
          SourceFileModule sourceFile = (SourceFileModule) file;
          
          args.add(sourceFile.getAbsolutePath());
        }
      }

      int i = 1;
      for (String arg : args) {
        LOG.info("arg" + i + " : " + arg);
        i++;
      }

      int execCode = jChecker.run(args.toArray(new String[args.size()]));
      LOG.info("Checker completed with execCode " + execCode);
      
      //ANALYZE
      try {
        Collection<AnalysisResult> results = new ArrayList(Collections.emptyList());
        for (CodeAnalysis analysis : analysisList) {
          for (Module file : files) {
            if (file instanceof SourceFileModule) {
              SourceFileModule sourceFile = (SourceFileModule) file;
              final URL clientURL = new URL(server.getClientUri(sourceFile.getURL().toString()));

              results.addAll(analyze(sourceFile, clientURL, analysis, jChecker));
            }
          }
        }

        server.cleanUp();
        server.consume(results, source());
      } catch(MalformedURLException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * set up source code path and library path with the project service provided by the server.
   *
   * @param server
   */
  public void setClassPath(MagpieServer server) {
    if (srcPath == null) {
      Optional<IProjectService> opt = server.getProjectService("java");
      if (opt.isPresent()) {
        JavaProjectService ps = (JavaProjectService) server.getProjectService("java").get();
        Set<Path> sourcePath = ps.getSourcePath();

        if (libPath == null) {
          libPath = new HashSet<>();
          ps.getLibraryPath().stream().forEach(path -> libPath.add(path.toString()));
        }
        if (!sourcePath.isEmpty()) {
          Set<String> temp = new HashSet<>();
          sourcePath.stream().forEach(path -> temp.add(path.toString()));
          srcPath = temp;

          Set<Path> cp = ps.getClassPath();
          totalClassPath = "\".";
          for (Path p : cp) {
            LOG.info("cp: " + p.toString());
            totalClassPath += ";" + p.toString();
            if(new File(p.toString()).isDirectory()) totalClassPath += "/**.jar";
          }

          totalClassPath += ";" + srcPath.iterator().next() + "/**.java\"";
        }
      }
    }
  }


  public Collection<AnalysisResult> analyze(SourceFileModule file, URL clientURL, CodeAnalysis analysis, IntraJ jChecker) {

    for (CompilationUnit cu : jChecker.getEntryPoint().getCompilationUnits()) {
      if(cu.getClassSource().sourceName().equals(file.getAbsolutePath())) {
        LOG.info("Performing analysis");
        analysis.doAnalysis(cu, clientURL);
      }
    }
    return analysis.getResult();
  }

  public static void addAnalysis(CodeAnalysis analysis) {
    analysisList.add(analysis);
  }

  private Collection<String> listFilesForFolder(final File folder) {
    Collection<String> files = new HashSet<>();

    for (final File fileEntry : folder.listFiles()) {
        if (fileEntry.isDirectory()) {
            files.addAll(listFilesForFolder(fileEntry));
        } else {
            files.add(fileEntry.getAbsolutePath());
        }
    }

    return files;
}

}
