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

import com.github.javaparser.utils.Log;
import com.ibm.wala.classLoader.Module;

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
public class SimpleServerAnalysis implements ServerAnalysis {
  private static final Logger LOG = Logger.getLogger("main");
  private Set<String> srcPath;
  private Set<String> libPath;
  private ExecutorService exeService;
  private Future<?> last;

  public SimpleServerAnalysis() {
    exeService = Executors.newSingleThreadExecutor();
  }

  @Override
  public String source() {
    return "String equality checker";
  }

  @Override
  public void analyze(Collection<? extends Module> files, AnalysisConsumer consumer, boolean rerun) {
    // if (last != null && !last.isDone()) {
    //   last.cancel(false);
    //   if (last.isCancelled())
    //     LOG.info("Susscessfully cancelled last analysis and start new");
    // }

    // Future<?> future = exeService.submit(new Runnable() {
    //   @Override
    //   public void run() {

        LOG.info("starting analysis");
        MagpieServer server=(MagpieServer) consumer;
        setClassPath(server);
        Collection<AnalysisResult> results = Collections.emptyList();
        if (srcPath != null) {
          results = analyze(srcPath, libPath);
        }

        server.consume(results, source());
    //   }
    // });
    // last = future;
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
        }
      }
    }
  }


  public Collection<AnalysisResult> analyze(Set<String> srcPath, Set<String> libPath) {
    Collection<AnalysisResult> results = new HashSet<>();

    IntraJ jChecker = new IntraJ();

    String[] args = {srcPath.iterator().next()};
    if(!srcPath.isEmpty())
      args[0] += "\\Test.java";

    int execCode = jChecker.run(args);

    LOG.info("Hello User");
    LOG.info("Executing analysis on " + args[0]);
    LOG.info("Nr of args: " + args.length);
    LOG.info("ExtendJ compilation code " + String.valueOf(execCode));

    StringEqAnalysis analysis = new StringEqAnalysis();
    analysis.doAnalysis(jChecker.getEntryPoint());
    results.addAll(analysis.getResult());

    return results;
  }
}
