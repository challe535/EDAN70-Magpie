package intrep.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;

import intrep.eval.SimpleFileWriter;
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.IProjectService;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.analysis.configuration.ConfigurationOption;
import magpiebridge.core.analysis.configuration.OptionType;
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
  private Collection<Future<?>> last;
  private Boolean shouldEval = false;

  private AnalysisFramework framework;

  private static Map<String, Boolean> activeAnalyses;
  private static Collection<CodeAnalysis<?>> analysisList;

  private int iterations = 1;
  private double[] timings;

  public StaticServerAnalysis() {
    exeService = Executors.newSingleThreadExecutor();
    analysisList = new ArrayList<CodeAnalysis<?>>();
    progFilesAbsPaths = new HashSet<>();
    last = new ArrayList<>(analysisList.size());
    activeAnalyses = new HashMap<>();

    //framework = new IntraJFramework();
    framework = new IntraJFramework();
  }

  @Override
  public String source() {
    return "Interactive Repair";
  }

  @Override
  public void analyze(Collection<? extends Module> files, AnalysisConsumer consumer, boolean rerun) {
    if(rerun) {
      if(shouldEval) {
        LOG.info("\n==================\nSTARTING ANALYSIS\n==================\n");
        timings = new double[iterations];

        for(int i = 0; i < iterations; i++) {
          double time = System.currentTimeMillis();

          //if analysis iteration fails - redo iteration
          if(!doSingleAnalysisIteration(files, consumer)) {
            i--;
            continue;
          }

          //Wait for all analysis threads
          boolean done = true;
          while(!done) {
            boolean temp = true;
            for(Future<?> f : last) {
              temp &= f.isDone();
            }
            done = temp;
          }

          //record time
          timings[i] = System.currentTimeMillis() - time;
        }

        //Write recorded data to file
        SimpleFileWriter writer = new SimpleFileWriter("test.txt");
        for(int i = 0; i < iterations; i++) {
          writer.appendLn(String.valueOf(timings[i]));
        }
        writer.close();

        LOG.info("\n==================\nFINISHED ANALYSIS\n==================\n");
      } else {
        doSingleAnalysisIteration(files, consumer);
      }
    }
  }

  private boolean doSingleAnalysisIteration(Collection<? extends Module> files, AnalysisConsumer consumer) {
    MagpieServer server = (MagpieServer) consumer;
    progFilesAbsPaths.clear();
    setClassPath(server, files);
   
    //Setup analysis framework and run
    framework.setup(files, totalClassPath, srcPath, libPath, progFilesAbsPaths); 
    int exitCode = framework.run();

    LOG.info("Framework run completed with exitCode " + exitCode);
    if(shouldEval && exitCode == 4) {
      return false;
    }
    
    //ANALYZE

    //Clean up previous analysis results and ongoing analyses
    server.cleanUp();
    for(Future<?> f : last) {
      if(f != null && !f.isDone()) {
        f.cancel(false);
      if (f.isCancelled())
        LOG.info("Susscessfully cancelled last analysis and start new");
      }
    }
    last.clear();

    //Initiate analyses on seperate threads and set them running
    for (CodeAnalysis analysis : analysisList) {
      if(!activeAnalyses.get(analysis.getName()))
        continue;

      last.add(exeService.submit(new Runnable() {
        @Override
        public void run() {
          Collection<AnalysisResult> results = new ArrayList<>(Collections.emptyList());
          for (Module file : files) {
            if (file instanceof SourceFileModule) {
              SourceFileModule sourceFile = (SourceFileModule) file;
              try {
                final URL clientURL = new URL(server.getClientUri(sourceFile.getURL().toString()));
                results.addAll(framework.analyze(sourceFile, clientURL, analysis));
              } catch(MalformedURLException e) {
                e.printStackTrace();
              }
            }
          }
          server.consume(results, source());
        }
      }));
    }

    return true;
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

  public static void addAnalysis(CodeAnalysis<?> analysis) {
    analysisList.add(analysis);
    activeAnalyses.put(analysis.getName(), true);
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

  @Override
  public List<ConfigurationOption> getConfigurationOptions() { 
    List<ConfigurationOption> options = new ArrayList<>();
    ConfigurationOption evalActive = new ConfigurationOption("Evaluation mode", OptionType.checkbox); 
    ConfigurationOption evalIter = new ConfigurationOption("Evalutaion iterations", OptionType.text, "1");
    evalActive.addChild(evalIter);

    ConfigurationOption analyses = new ConfigurationOption("Analyses", OptionType.container);

    for (CodeAnalysis a : analysisList) {
      analyses.addChild(new ConfigurationOption(a.getName(), OptionType.checkbox, "true"));
    }

    options.add(evalActive);
    options.add(analyses);

    return options;
  }

  @Override
  public void configure(List<ConfigurationOption> configuration) {
      for (ConfigurationOption o : configuration){
          switch(o.getName()) {
            case "Evaluation mode": shouldEval = o.getValueAsBoolean(); 
              if(shouldEval) 
                iterations = Integer.parseInt(o.getChildren().get(0).getValue());
              break;
            case "Evaluation iterations": ;
              LOG.info(String.valueOf(iterations));
              break;
            case "Analyses": 
              for(ConfigurationOption c : o.getChildren()) {
                activeAnalyses.put(c.getName(), c.getValueAsBoolean());
              }
              break;
            default:
              LOG.warning("No configuration case mathcing " + o.getName());
          }    
      } 
  }

}
