package intrep.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.security.cert.PKIXRevocationChecker.Option;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
  private Set<String> progFilesAbsPaths;
  private String totalClassPath;
  private ExecutorService exeService;
  private Future<?> last;

  private static Collection<CodeAnalysis> analysisList;

  public StaticServerAnalysis() {
    exeService = Executors.newSingleThreadExecutor();
    analysisList = new ArrayList<CodeAnalysis>();
    progFilesAbsPaths = new HashSet<>();
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

      progFilesAbsPaths.clear();
      setClassPath(server, files);

      Collection<String> args = new LinkedHashSet<String>();

      args.add("-nowarn");

      if(!totalClassPath.equals("")) {
        args.add("-classpath");
        args.add(totalClassPath);
      }

      for (String path : progFilesAbsPaths) {
        args.add(path);
      }

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
      server.cleanUp();
      try {
        for (CodeAnalysis analysis : analysisList) {
          Collection<AnalysisResult> results = new ArrayList(Collections.emptyList());

          for (Module file : files) {
            if (file instanceof SourceFileModule) {
              SourceFileModule sourceFile = (SourceFileModule) file;
              final URL clientURL = new URL(server.getClientUri(sourceFile.getURL().toString()));

              results.addAll(analyze(sourceFile, clientURL, analysis, jChecker));
            }
          }
          server.consume(results, source());
        }
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
  public void setClassPath(MagpieServer server, Collection<? extends Module> files) {
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
          totalClassPath = ".";

          Set<String> temp = new HashSet<>();
          sourcePath.stream().forEach(path -> temp.add(path.toString()));
          srcPath = temp;

          Set<String> requestedFiles = new HashSet<>();
          for (Module file : files) {
            if (file instanceof SourceFileModule) {
              SourceFileModule sourceFile = (SourceFileModule) file;
              
              requestedFiles.add(sourceFile.getClassName() + ".java");
            }
          }
 
          Iterator<String> srcIt = srcPath.iterator();

          if(srcIt.hasNext()) {
            String src =  srcIt.next();
            Collection<String> srcJavas = getJavaFilesForFolder(new File(src), ".java");
            for (String javaPath : srcJavas) {
              if(!requestedFiles.contains(getFileNameFromPath(javaPath)) && !progFilesAbsPaths.contains(javaPath)) 
                progFilesAbsPaths.add(javaPath);
            }
          }

          if(!libPath.isEmpty()) {
            Iterator<String> libIt = libPath.iterator();
            while(libIt.hasNext()) {
              String lib =  libIt.next();             
              Set<String> libJars = new HashSet<>(getJavaFilesForFolder(new File(lib), ".jar"));
              for (String jarPath : libJars) {
                totalClassPath += ";" + jarPath;
              }
            }
          }

          Set<Path> cp = ps.getClassPath();
          for (Path p : cp) {
            totalClassPath += ";" + p.toString();
          }

          Optional<Path> root = ps.getRootPath();
          if(root.isPresent()) {
            String rootPath = root.get().toString();
            Set<String> ecp = new HashSet<>(getJavaFilesForFolder(new File(rootPath), ".jar"));
            for (String p : ecp) {
              totalClassPath += ";" + p;
            }
          }
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

  private Collection<String> getJavaFilesForFolder(final File folder, String ext) {
    Collection<String> files = new HashSet<>();
    if(folder.isDirectory()) {
      for (final File fileEntry : folder.listFiles()) {
        if (fileEntry.isDirectory()) {
          files.addAll(getJavaFilesForFolder(fileEntry, ext));
        } else if(fileEntry.getName().endsWith(ext)){
          files.add(fileEntry.getAbsolutePath());
        }
      }
    } else if(folder.getName().endsWith(ext)) {
      files.add(folder.getAbsolutePath());
    }

    return files;
  }

  private String getFileNameFromPath(String path) {
    File f = new File(path);
    if(f.isFile())
      return f.getName();
    return "";
  }

}
